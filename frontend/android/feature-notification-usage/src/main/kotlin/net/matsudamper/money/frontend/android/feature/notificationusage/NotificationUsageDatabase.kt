package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NotificationUsageEntity::class],
    version = 4,
    exportSchema = false,
)
internal abstract class NotificationUsageDatabase : RoomDatabase() {
    abstract fun notificationUsageDao(): NotificationUsageDao

    companion object {
        val Migration1To2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE notification_usage_records
                    ADD COLUMN isAdded INTEGER NOT NULL DEFAULT 0
                    """.trimIndent(),
                )
            }
        }

        val Migration2To3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE notification_usage_records
                    ADD COLUMN moneyUsageId INTEGER DEFAULT NULL
                    """.trimIndent(),
                )
            }
        }

        val Migration3To4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE notification_usage_records
                    ADD COLUMN notificationMetadata TEXT NOT NULL DEFAULT ''
                    """.trimIndent(),
                )
            }
        }
    }
}
