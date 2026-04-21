package com.lucidera.investigations.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportFactory
import com.lucidera.investigations.data.local.entity.CaseAttachmentEntity
import com.lucidera.investigations.data.local.entity.EntityProfileEntity
import com.lucidera.investigations.data.local.entity.InvestigationCaseEntity
import com.lucidera.investigations.data.local.entity.LeadEntity

@Database(
    entities = [InvestigationCaseEntity::class, LeadEntity::class, EntityProfileEntity::class, CaseAttachmentEntity::class],
    version = 8,
    exportSchema = true
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema was identical between v1 and v2; version bump only.
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `case_attachments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `caseId` INTEGER NOT NULL,
                        `uri` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `caption` TEXT NOT NULL,
                        `attachmentType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`caseId`) REFERENCES `cases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_case_attachments_caseId` ON `case_attachments` (`caseId`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN gpsLat REAL")
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN gpsLon REAL")
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN capturedAt INTEGER")
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN deviceModel TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN mimeType TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leads ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE entity_profiles ADD COLUMN aliases TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN fileHash TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE case_attachments ADD COLUMN transcription TEXT")
            }
        }

        fun getDatabase(context: Context): FieldbookDatabase =
            INSTANCE ?: synchronized(this) {
                val passphrase = CryptoManager.getPassphrase(context).toByteArray()
                val factory = SupportFactory(passphrase)
                
                Room.databaseBuilder(
                    context.applicationContext,
                    FieldbookDatabase::class.java,
                    "fieldbook.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
