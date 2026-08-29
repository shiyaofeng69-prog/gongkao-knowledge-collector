package com.gongkao.collector.domain.model

data class SourceRecord(
    val id: String,
    val kind: SourceKind,
    val originalText: String?,
    val imagePath: String?,
    val sha256: String?,
    val status: SourceStatus,
    val createdAt: Long,
    val deletedAt: Long? = null,
) {
    init {
        require(id.isNotBlank()) { "Source id must not be blank" }
        require(originalText != null || imagePath != null) {
            "A source must retain text or an application-private image path"
        }
    }
}
