package com.gongkao.collector.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface FoundationDao {
    @Insert
    suspend fun insertSource(source: SourceRecordEntity)

    @Query("SELECT COUNT(*) FROM source_record")
    suspend fun countSources(): Int

    @Query("SELECT * FROM source_record WHERE id = :id")
    suspend fun findSourceById(id: String): SourceRecordEntity?

    @Query("SELECT * FROM source_record WHERE sha256 = :sha256 ORDER BY createdAt ASC LIMIT 1")
    suspend fun findFirstSourceByHash(sha256: String): SourceRecordEntity?

    @Query("SELECT * FROM source_record ORDER BY createdAt DESC")
    suspend fun listSources(): List<SourceRecordEntity>

    @Transaction
    suspend fun insertSourceThenFail(source: SourceRecordEntity) {
        insertSource(source)
        error("Deliberate failure used to verify transaction rollback")
    }
}
