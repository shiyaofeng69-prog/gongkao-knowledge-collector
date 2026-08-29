package com.gongkao.collector.data.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class GongkaoDatabaseTest {
    private lateinit var database: GongkaoDatabase

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder<GongkaoDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `empty database creates every phase one table and required index`() = runTest {
        val tables = database.useReaderConnection { connection ->
            connection.usePrepared(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'room_%' AND name NOT LIKE 'sqlite_%'",
            ) { statement ->
                buildSet {
                    while (statement.step()) add(statement.getText(0))
                }
            }
        }

        assertEquals(
            setOf(
                "source_record",
                "processing_job",
                "ocr_document",
                "question",
                "question_option",
                "knowledge_item",
                "question_knowledge",
                "knowledge_evidence",
                "question_evidence",
                "tag",
                "knowledge_tag",
                "review_item",
            ),
            tables,
        )

        val indexNames = database.useReaderConnection { connection ->
            connection.usePrepared("SELECT name FROM sqlite_master WHERE type = 'index'") { statement ->
                buildSet {
                    while (statement.step()) add(statement.getText(0))
                }
            }
        }
        assertTrue(indexNames.any { it.contains("source_record_sha256") })
        assertTrue(indexNames.any { it.contains("knowledge_item_type_canonicalKey") })
        assertTrue(indexNames.any { it.contains("question_option_questionId_label") })
        assertTrue(indexNames.any { it.contains("review_item_status_createdAt") })
    }

    @Test
    fun `failed transaction leaves no partial source`() = runTest {
        val source = SourceRecordEntity(
            id = "source-rollback",
            kind = "TEXT",
            originalText = "事务回滚测试",
            imagePath = null,
            sha256 = null,
            status = "SAVED",
            createdAt = 1L,
            deletedAt = null,
        )

        runCatching { database.foundationDao().insertSourceThenFail(source) }

        assertEquals(0, database.foundationDao().countSources())
    }
}
