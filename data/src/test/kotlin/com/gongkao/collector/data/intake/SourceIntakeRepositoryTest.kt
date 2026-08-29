package com.gongkao.collector.data.intake

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gongkao.collector.data.db.GongkaoDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SourceIntakeRepositoryTest {
    private lateinit var database: GongkaoDatabase
    private lateinit var sourceDirectory: File
    private lateinit var repository: SourceIntakeRepository
    private var idSequence = 0

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder<GongkaoDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        sourceDirectory = File(context.cacheDir, "intake-test-${System.nanoTime()}")
        repository = SourceIntakeRepository(
            sourceDirectory = sourceDirectory,
            dao = database.foundationDao(),
            now = { 1234L },
            newId = { "source-${++idSequence}" },
        )
    }

    @After
    fun tearDown() {
        database.close()
        sourceDirectory.deleteRecursively()
    }

    @Test
    fun `M01-003 trims outer whitespace but keeps internal Chinese line breaks`() = runTest {
        val result = repository.importText("  第一行\n  第二行  \n第三行  ") as IntakeResult.Saved

        assertEquals("第一行\n  第二行  \n第三行", result.source.originalText)
        assertEquals("text/plain", result.source.mimeType)
        assertEquals(1, database.foundationDao().countSources())
    }

    @Test
    fun `M01-004 rejects blank paste without creating a source`() = runTest {
        val result = repository.importText(" \n\t ")

        assertEquals(IntakeResult.Rejected(IntakeError.EMPTY_TEXT), result)
        assertEquals(0, database.foundationDao().countSources())
    }

    @Test
    fun `M01-001 imports PNG to private storage with metadata`() = runTest {
        val bytes = imageBytes(Bitmap.CompressFormat.PNG, width = 80, height = 120)
        val result = repository.importImage("image/png") { ByteArrayInputStream(bytes) } as IntakeResult.Saved

        val privateCopy = File(requireNotNull(result.source.imagePath))
        assertTrue(privateCopy.isFile)
        assertArrayEquals(bytes, privateCopy.readBytes())
        assertEquals("image/png", result.source.mimeType)
        assertEquals(80, result.source.width)
        assertEquals(120, result.source.height)
        assertEquals(bytes.size.toLong(), result.source.byteSize)
        assertEquals(sha256(bytes), result.source.sha256)
    }

    @Test
    fun `M01-002 imports JPEG and preserves its MIME dimensions and hash`() = runTest {
        val bytes = imageBytes(Bitmap.CompressFormat.JPEG, width = 96, height = 64)
        val result = repository.importImage("image/jpeg") { ByteArrayInputStream(bytes) } as IntakeResult.Saved

        assertEquals("image/jpeg", result.source.mimeType)
        assertEquals(96, result.source.width)
        assertEquals(64, result.source.height)
        assertEquals(sha256(bytes), result.source.sha256)
    }

    @Test
    fun `M01-005 rejects unsupported MIME without leaving a partial file`() = runTest {
        val result = repository.importImage("application/pdf") { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }

        assertEquals(IntakeResult.Rejected(IntakeError.UNSUPPORTED_MIME), result)
        assertFalse(sourceDirectory.exists())
        assertEquals(0, database.foundationDao().countSources())
    }

    @Test
    fun `M01-007 private copy remains readable after the external stream is gone`() = runTest {
        val bytes = imageBytes(Bitmap.CompressFormat.PNG, width = 48, height = 48)
        var externalBytes: ByteArray? = bytes
        val result = repository.importImage("image/png") {
            ByteArrayInputStream(requireNotNull(externalBytes))
        } as IntakeResult.Saved

        externalBytes = null
        assertArrayEquals(bytes, File(requireNotNull(result.source.imagePath)).readBytes())
    }

    @Test
    fun `M01-008 long screenshot uses bounded decode sample and keeps original`() = runTest {
        val bytes = imageBytes(Bitmap.CompressFormat.PNG, width = 32, height = 10_000)
        val result = repository.importImage("image/png") { ByteArrayInputStream(bytes) } as IntakeResult.Saved

        assertEquals(4, result.source.decodeSampleSize)
        assertArrayEquals(bytes, File(requireNotNull(result.source.imagePath)).readBytes())
    }

    @Test
    fun `M01-009 exact duplicate records a signal but keeps both sources`() = runTest {
        val bytes = imageBytes(Bitmap.CompressFormat.PNG, width = 40, height = 40)
        val first = repository.importImage("image/png") { ByteArrayInputStream(bytes) } as IntakeResult.Saved
        val second = repository.importImage("image/png") { ByteArrayInputStream(bytes) } as IntakeResult.Saved

        assertNull(first.source.duplicateOfSourceId)
        assertEquals(first.source.id, second.source.duplicateOfSourceId)
        assertEquals(2, database.foundationDao().countSources())
    }

    private fun imageBytes(format: Bitmap.CompressFormat, width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        return ByteArrayOutputStream().use { output ->
            assertTrue(bitmap.compress(format, 90, output))
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
