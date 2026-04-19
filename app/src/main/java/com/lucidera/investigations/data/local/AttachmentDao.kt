package com.lucidera.investigations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: CaseAttachmentEntity): Long

    @Query("SELECT * FROM case_attachments WHERE caseId = :caseId ORDER BY createdAt DESC")
    fun observeAttachmentsForCase(caseId: Long): Flow<List<CaseAttachmentEntity>>
}
