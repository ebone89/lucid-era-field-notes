package com.lucidera.investigations.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.data.local.entity.LeadEntity

@Database(
    entities = [InvestigationCaseEntity::class, LeadEntity::class, EntityProfileEntity::class, CaseAttachmentEntity::class],
    version = 4,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN gpsLat REAL")
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN gpsLon REAL")
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN capturedAt INTEGER")
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN deviceModel TEXT")
            }
        }

        fun getDatabase(context: Context): FieldbookDatabase =
            INSTANCE ?: synchronized(this) {
                // fallbackToDestructiveMigration: any unhandled version bump wipes all local data.
                // Replace with explicit Migration objects before adding schema changes to a
                // build that has been installed on a device with real case data.
                Room.databaseBuilder(
                    context.applicationContext,
                    FieldbookDatabase::class.java,
                    "fieldbook.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
