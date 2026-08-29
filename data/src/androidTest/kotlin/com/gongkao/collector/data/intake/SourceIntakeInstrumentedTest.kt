package com.gongkao.collector.data.intake

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gongkao.collector.data.db.GongkaoDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class SourceIntakeInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "gate01-intake-device.db"
    private val sourceDirectory by lazy { File(context.cacheDir, "gate01-intake") }
    private lateinit var database: GongkaoDatabase
    private lateinit var repository: SourceIntakeRepository

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        sourceDirectory.deleteRecursively()
        database = GongkaoDatabase.create(context, databaseName)
        repository = SourceIntakeRepository(sourceDirectory, database.foundationDao())
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
        sourceDirectory.deleteRecursively()
    }

    @Test
    fun pngIsCopiedBeforeTheExternalStreamDisappears() = runTest {
        val bytes = imageBytes(Bitmap.CompressFormat.PNG, 72, 144)
        val saved = repository.importImage("image/png") { ByteArrayInputStream(bytes) } as IntakeResult.Saved

        assertEquals(72, saved.source.width)
        assertEquals(144, saved.source.height)
        assertTrue(File(requireNotNull(saved.source.imagePath)).isFile)
    }

    @Test
    fun jpegPreservesMimeAndUnsupportedContentLeavesNoFile() = runTest {
        val jpeg = imageBytes(Bitmap.CompressFormat.JPEG, 120, 80)
        val saved = repository.importImage("image/jpeg") { ByteArrayInputStream(jpeg) } as IntakeResult.Saved
        val rejected = repository.importImage("application/pdf") { ByteArrayInputStream(byteArrayOf(1)) }

        assertEquals("image/jpeg", saved.source.mimeType)
        assertEquals(IntakeResult.Rejected(IntakeError.UNSUPPORTED_MIME), rejected)
        assertEquals(1, database.foundationDao().countSources())
    }

    private fun imageBytes(format: Bitmap.CompressFormat, width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(format, 90, output))
            bitmap.recycle()
            output.toByteArray()
        }
    }
}
