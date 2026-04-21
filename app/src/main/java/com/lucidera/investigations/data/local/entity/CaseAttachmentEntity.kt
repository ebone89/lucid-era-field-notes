package com.lucidera.investigations.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lucidera.investigations.data.AttachmentType

@Entity(
    tableName = "case_attachments",
    foreignKeys = [
        ForeignKey(
            entity = InvestigationCaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("caseId")]
)
data class CaseAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val caseId: Long,
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val caption: String,
    val attachmentType: AttachmentType,
    val createdAt: Long = System.currentTimeMillis(),
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val capturedAt: Long? = null,
    val deviceModel: String? = null
)
