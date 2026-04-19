package com.lucidera.investigations.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lucidera.investigations.data.LeadStatus

@Entity(
    tableName = "leads",
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
data class LeadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val caseId: Long,
    val sourceName: String,
    val sourceUrl: String,
    val archiveUrl: String,
    val summary: String,
    val status: LeadStatus,
    val collectedAt: Long = System.currentTimeMillis()
)
