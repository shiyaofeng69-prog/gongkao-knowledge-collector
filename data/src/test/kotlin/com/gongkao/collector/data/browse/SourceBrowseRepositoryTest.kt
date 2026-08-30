package com.gongkao.collector.data.browse

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gongkao.collector.data.db.GongkaoDatabase
import com.gongkao.collector.data.db.SourceRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SourceBrowseRepositoryTest {
    private lateinit var database: GongkaoDatabase
    private lateinit var repository: SourceBrowseRepository

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder<GongkaoDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = SourceBrowseRepository(database.foundationDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `collection lists newest active sources first with deterministic tie break`() = runTest {
        database.foundationDao().insertSource(source(id = "older", createdAt = 10))
        database.foundationDao().insertSource(source(id = "newer-a", createdAt = 20))
        database.foundationDao().insertSource(source(id = "newer-b", createdAt = 20))

        assertEquals(listOf("newer-b", "newer-a", "older"), repository.listSources().map { it.id })
    }

    @Test
    fun `collection and detail hide soft deleted sources`() = runTest {
        database.foundationDao().insertSource(source(id = "active", createdAt = 10))
        database.foundationDao().insertSource(source(id = "deleted", createdAt = 20, deletedAt = 30))

        assertEquals(listOf("active"), repository.listSources().map { it.id })
        assertNull(repository.findSource("deleted"))
    }

    private fun source(id: String, createdAt: Long, deletedAt: Long? = null) = SourceRecordEntity(
        id = id,
        kind = "TEXT",
        originalText = "题目 $id",
        imagePath = null,
        sha256 = null,
        mimeType = "text/plain",
        status = "SAVED",
        createdAt = createdAt,
        deletedAt = deletedAt,
    )
}
