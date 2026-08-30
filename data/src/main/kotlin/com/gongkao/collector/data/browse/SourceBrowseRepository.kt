package com.gongkao.collector.data.browse

import com.gongkao.collector.data.db.FoundationDao
import com.gongkao.collector.data.db.SourceRecordEntity

class SourceBrowseRepository(
    private val dao: FoundationDao,
) {
    suspend fun listSources(): List<SourceRecordEntity> = dao.listSources()

    suspend fun findSource(id: String): SourceRecordEntity? = dao.findSourceById(id)
        ?.takeIf { it.deletedAt == null }
}
