package com.gongkao.collector.ai

import com.gongkao.collector.domain.model.KnowledgeType

/** Compile-time marker for the AI adapter boundary. No provider code belongs in Gate-00. */
object AiModule {
    val supportedTypes: Set<KnowledgeType> = KnowledgeType.entries.toSet()
}
