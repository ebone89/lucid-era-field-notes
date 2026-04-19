package com.lucidera.investigations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntityProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntity(entity: EntityProfileEntity): Long

    @Query("SELECT * FROM entity_profiles WHERE caseId = :caseId ORDER BY name ASC")
    fun observeEntitiesForCase(caseId: Long): Flow<List<EntityProfileEntity>>

    @Query("SELECT COUNT(*) FROM entity_profiles")
    fun observeEntityCount(): Flow<Int>
}
