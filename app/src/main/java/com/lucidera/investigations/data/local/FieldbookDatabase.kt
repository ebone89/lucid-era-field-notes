package com.lucidera.investigations.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.data.local.entity.LeadEntity

@Database(
    entities = [InvestigationCaseEntity::class, LeadEntity::class, EntityProfileEntity::class, CaseAttachmentEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FieldbookDatabase : RoomDatabase() {

    abstract fun caseDao(): CaseDao
    abstract fun leadDao(): LeadDao
    abstract fun entityProfileDao(): EntityProfileDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        @Volatile
        private var INSTANCE: FieldbookDatabase? = null

        fun getDatabase(context: Context): FieldbookDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FieldbookDatabase::class.java,
                    "fieldbook.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
