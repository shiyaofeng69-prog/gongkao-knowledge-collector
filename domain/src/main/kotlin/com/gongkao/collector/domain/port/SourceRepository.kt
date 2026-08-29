package com.gongkao.collector.domain.port

import com.gongkao.collector.domain.model.SourceRecord

interface SourceRepository {
    suspend fun save(source: SourceRecord)

    suspend fun findById(id: String): SourceRecord?
}
