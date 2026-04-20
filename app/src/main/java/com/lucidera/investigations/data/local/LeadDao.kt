package com.lucidera.investigations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lucidera.investigations.data.LeadStatus
import com.lucidera.investigations.data.local.entity.LeadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(leadEntity: LeadEntity): Long

    @Query("DELETE FROM leads WHERE id = :leadId")
    suspend fun deleteLead(leadId: Long)

    @Query("UPDATE leads SET status = :status WHERE id = :leadId")
    suspend fun updateLeadStatus(leadId: Long, status: LeadStatus)

    @Query("SELECT * FROM leads WHERE caseId = :caseId ORDER BY collectedAt DESC")
    fun observeLeadsForCase(caseId: Long): Flow<List<LeadEntity>>

    @Query("SELECT COUNT(*) FROM leads WHERE status = :status")
    fun observeLeadCountByStatus(status: LeadStatus): Flow<Int>
}
