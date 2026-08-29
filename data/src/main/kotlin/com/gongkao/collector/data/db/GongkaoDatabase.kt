package com.gongkao.collector.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.AutoMigration
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [
        SourceRecordEntity::class,
        ProcessingJobEntity::class,
        OcrDocumentEntity::class,
        QuestionEntity::class,
        QuestionOptionEntity::class,
        KnowledgeItemEntity::class,
        QuestionKnowledgeEntity::class,
        KnowledgeEvidenceEntity::class,
        QuestionEvidenceEntity::class,
        TagEntity::class,
        KnowledgeTagEntity::class,
        ReviewItemEntity::class,
    ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true,
)
abstract class GongkaoDatabase : RoomDatabase() {
    abstract fun foundationDao(): FoundationDao

    companion object {
        const val DATABASE_NAME = "gongkao.db"

        fun create(context: Context, name: String = DATABASE_NAME): GongkaoDatabase =
            Room.databaseBuilder<GongkaoDatabase>(context.applicationContext, name)
                .setDriver(AndroidSQLiteDriver())
                .build()
    }
}
