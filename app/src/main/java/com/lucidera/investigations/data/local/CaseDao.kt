package com.lucidera.investigations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {

    @Query("SELECT * FROM cases ORDER BY updatedAt DESC")
    fun observeAllCases(): Flow<List<InvestigationCaseEntity>>

    @Query("SELECT * FROM cases WHERE id = :caseId")
    fun observeCase(caseId: Long): Flow<InvestigationCaseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(caseEntity: InvestigationCaseEntity): Long

    @Query("DELETE FROM cases WHERE id = :caseId")
    suspend fun deleteCase(caseId: Long)

    @Query("SELECT COUNT(*) FROM cases")
    suspend fun countCases(): Int
}
