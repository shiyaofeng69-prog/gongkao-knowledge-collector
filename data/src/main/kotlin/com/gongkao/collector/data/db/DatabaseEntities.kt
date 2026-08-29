package com.gongkao.collector.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "source_record",
    indices = [Index(value = ["sha256"])],
)
data class SourceRecordEntity(
    @androidx.room3.PrimaryKey val id: String,
    val kind: String,
    val originalText: String?,
    val imagePath: String?,
    val sha256: String?,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val byteSize: Long? = null,
    val decodeSampleSize: Int? = null,
    val duplicateOfSourceId: String? = null,
    val status: String,
    val createdAt: Long,
    val deletedAt: Long?,
)

@Entity(
    tableName = "processing_job",
    foreignKeys = [
        ForeignKey(
            entity = SourceRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sourceId"])],
)
data class ProcessingJobEntity(
    @androidx.room3.PrimaryKey val id: String,
    val sourceId: String,
    val stage: String,
    val attempt: Int,
    val provider: String?,
    val model: String?,
    val errorCode: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "ocr_document",
    foreignKeys = [
        ForeignKey(
            entity = SourceRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class OcrDocumentEntity(
    @androidx.room3.PrimaryKey val sourceId: String,
    val plainText: String,
    val blocksJson: String,
    val engineVersion: String,
)

@Entity(
    tableName = "question",
    foreignKeys = [
        ForeignKey(
            entity = SourceRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sourceId"], unique = true),
        Index(value = ["normalizedStem"]),
    ],
)
data class QuestionEntity(
    @androidx.room3.PrimaryKey val id: String,
    val sourceId: String,
    val stem: String,
    val normalizedStem: String,
    val category: String,
    val deletedAt: Long?,
)

@Entity(
    tableName = "question_option",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["questionId", "label"], unique = true)],
)
data class QuestionOptionEntity(
    @androidx.room3.PrimaryKey val id: String,
    val questionId: String,
    val label: String,
    val originalText: String,
    val normalizedText: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "knowledge_item",
    indices = [
        Index(value = ["type", "canonicalKey"], unique = true),
        Index(value = ["type", "status", "createdAt"]),
        Index(value = ["searchText"]),
    ],
)
data class KnowledgeItemEntity(
    @androidx.room3.PrimaryKey val id: String,
    val type: String,
    val canonicalKey: String,
    val title: String,
    val summary: String,
    val searchText: String,
    val status: String,
    val createdAt: Long,
    val deletedAt: Long?,
)

@Entity(
    tableName = "question_knowledge",
    primaryKeys = ["questionId", "knowledgeId"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KnowledgeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["knowledgeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = QuestionOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["optionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["questionId"]),
        Index(value = ["knowledgeId"]),
        Index(value = ["optionId"]),
    ],
)
data class QuestionKnowledgeEntity(
    val questionId: String,
    val knowledgeId: String,
    val optionId: String?,
    val role: String,
    val relationReason: String,
)

@Entity(
    tableName = "knowledge_evidence",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["knowledgeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["knowledgeId"])],
)
data class KnowledgeEvidenceEntity(
    @androidx.room3.PrimaryKey val id: String,
    val knowledgeId: String,
    val fieldKey: String,
    val value: String,
    val sourceType: String,
    val sourceRef: String?,
    val verification: String,
)

@Entity(
    tableName = "question_evidence",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["questionId"])],
)
data class QuestionEvidenceEntity(
    @androidx.room3.PrimaryKey val id: String,
    val questionId: String,
    val fieldKey: String,
    val value: String,
    val sourceType: String,
    val sourceRef: String?,
    val verification: String,
)

@Entity(
    tableName = "tag",
    indices = [Index(value = ["type", "canonicalKey"], unique = true)],
)
data class TagEntity(
    @androidx.room3.PrimaryKey val id: String,
    val name: String,
    val type: String,
    val canonicalKey: String,
)

@Entity(
    tableName = "knowledge_tag",
    primaryKeys = ["knowledgeId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["knowledgeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tagId"])],
)
data class KnowledgeTagEntity(
    val knowledgeId: String,
    val tagId: String,
    val sourceType: String,
    val verification: String,
)

@Entity(
    tableName = "review_item",
    indices = [
        Index(value = ["status", "createdAt"]),
        Index(value = ["targetType", "targetId"]),
    ],
)
data class ReviewItemEntity(
    @androidx.room3.PrimaryKey val id: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val severity: String,
    val status: String,
    val createdAt: Long,
)
