package com.lucidera.investigations.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lucidera.investigations.data.CaseStatus

@Entity(tableName = "cases")
data class InvestigationCaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val caseCode: String,
    val title: String,
    val essentialQuestion: String,
    val primarySubject: String,
    val status: CaseStatus,
    val classification: String,
    val leadInvestigator: String,
    val summary: String,
    val caseFolderName: String,
    val masterNoteName: String,
    val savePath: String,
    val publicationThreshold: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
