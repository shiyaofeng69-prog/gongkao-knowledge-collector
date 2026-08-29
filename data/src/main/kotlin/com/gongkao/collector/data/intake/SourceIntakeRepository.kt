package com.gongkao.collector.data.intake

import android.graphics.BitmapFactory
import com.gongkao.collector.data.db.FoundationDao
import com.gongkao.collector.data.db.SourceRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

class SourceIntakeRepository(
    private val sourceDirectory: File,
    private val dao: FoundationDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun importText(rawText: String): IntakeResult = withContext(Dispatchers.IO) {
        val normalized = rawText.trim()
        if (normalized.isBlank()) {
            return@withContext IntakeResult.Rejected(IntakeError.EMPTY_TEXT)
        }

        val source = SourceRecordEntity(
            id = newId(),
            kind = "TEXT",
            originalText = normalized,
            imagePath = null,
            sha256 = sha256(normalized.toByteArray(Charsets.UTF_8)),
            mimeType = "text/plain",
            byteSize = normalized.toByteArray(Charsets.UTF_8).size.toLong(),
            status = "SAVED",
            createdAt = now(),
            deletedAt = null,
        )
        dao.insertSource(source)
        IntakeResult.Saved(source)
    }

    suspend fun importImage(
        mimeType: String?,
        openStream: () -> InputStream?,
    ): IntakeResult = withContext(Dispatchers.IO) {
        val extension = when (mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            else -> return@withContext IntakeResult.Rejected(IntakeError.UNSUPPORTED_MIME)
        }

        sourceDirectory.mkdirs()
        val id = newId()
        val partialFile = File(sourceDirectory, "$id.$extension.part")
        val finalFile = File(sourceDirectory, "$id.$extension")

        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val byteSize = openStream()?.use { input ->
                partialFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        total += count
                    }
                    total
                }
            } ?: return@withContext IntakeResult.Rejected(IntakeError.UNREADABLE_SOURCE)

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(partialFile.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                partialFile.delete()
                return@withContext IntakeResult.Rejected(IntakeError.INVALID_IMAGE)
            }

            if (!partialFile.renameTo(finalFile)) {
                partialFile.delete()
                return@withContext IntakeResult.Rejected(IntakeError.STORAGE_FAILURE)
            }

            val hash = digest.digest().toHex()
            val duplicate = dao.findFirstSourceByHash(hash)
            val source = SourceRecordEntity(
                id = id,
                kind = "IMAGE",
                originalText = null,
                imagePath = finalFile.absolutePath,
                sha256 = hash,
                mimeType = mimeType.lowercase(),
                width = options.outWidth,
                height = options.outHeight,
                byteSize = byteSize,
                decodeSampleSize = calculateDecodeSampleSize(options.outWidth, options.outHeight),
                duplicateOfSourceId = duplicate?.id,
                status = "SAVED",
                createdAt = now(),
                deletedAt = null,
            )

            try {
                dao.insertSource(source)
            } catch (error: Throwable) {
                finalFile.delete()
                throw error
            }
            IntakeResult.Saved(source)
        } catch (error: Throwable) {
            partialFile.delete()
            if (error is kotlinx.coroutines.CancellationException) throw error
            IntakeResult.Rejected(IntakeError.STORAGE_FAILURE)
        }
    }

    companion object {
        const val MAX_DECODE_DIMENSION = 4096

        fun calculateDecodeSampleSize(width: Int, height: Int): Int {
            var sampleSize = 1
            while (width / sampleSize > MAX_DECODE_DIMENSION || height / sampleSize > MAX_DECODE_DIMENSION) {
                sampleSize *= 2
            }
            return sampleSize
        }

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

        private fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}

sealed interface IntakeResult {
    data class Saved(val source: SourceRecordEntity) : IntakeResult
    data class Rejected(val error: IntakeError) : IntakeResult
}

enum class IntakeError {
    EMPTY_TEXT,
    UNSUPPORTED_MIME,
    UNREADABLE_SOURCE,
    INVALID_IMAGE,
    STORAGE_FAILURE,
    MULTIPLE_IMAGES_NOT_SUPPORTED,
}
