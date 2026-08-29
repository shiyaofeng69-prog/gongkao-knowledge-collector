package com.gongkao.collector.ai

import com.gongkao.collector.domain.model.KnowledgeType
import org.junit.Assert.assertEquals
import org.junit.Test

class AiModuleTest {
    @Test
    fun `AI boundary consumes only domain types`() {
        assertEquals(KnowledgeType.entries.toSet(), AiModule.supportedTypes)
    }
}
