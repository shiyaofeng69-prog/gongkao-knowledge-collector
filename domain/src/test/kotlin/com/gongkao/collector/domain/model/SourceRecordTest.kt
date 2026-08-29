package com.gongkao.collector.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceRecordTest {
    @Test
    fun `domain model is constructible on plain JVM`() {
        val source = SourceRecord(
            id = "source-1",
            kind = SourceKind.TEXT,
            originalText = "题目文本",
            imagePath = null,
            sha256 = null,
            status = SourceStatus.SAVED,
            createdAt = 1L,
        )

        assertEquals(SourceStatus.SAVED, source.status)
    }

    @Test
    fun `source always retains recoverable original input`() {
        assertFailsWith<IllegalArgumentException> {
            SourceRecord(
                id = "source-2",
                kind = SourceKind.TEXT,
                originalText = null,
                imagePath = null,
                sha256 = null,
                status = SourceStatus.SAVED,
                createdAt = 1L,
            )
        }
    }
}
