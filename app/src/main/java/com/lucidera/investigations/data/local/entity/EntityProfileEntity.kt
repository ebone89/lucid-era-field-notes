package com.lucidera.investigations.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lucidera.investigations.data.ConfidenceLevel
import com.lucidera.investigations.data.EntityType

@Entity(
    tableName = "entity_profiles",
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
data class EntityProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val caseId: Long,
    val name: String,
    val entityType: EntityType,
    val confidence: ConfidenceLevel,
    val aliases: String = "",
    val summary: String,
    val identifier: String
)
