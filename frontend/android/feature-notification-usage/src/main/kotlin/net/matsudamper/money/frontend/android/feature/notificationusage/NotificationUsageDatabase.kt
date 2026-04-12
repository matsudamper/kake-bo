package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NotificationUsageEntity::class, NotificationUsageLinkedUsageEntity::class],
    version = 5,
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

        val Migration4To5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_usage_linked_usages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        notificationKey TEXT NOT NULL,
                        moneyUsageId INTEGER NOT NULL,
                        FOREIGN KEY (notificationKey) REFERENCES notification_usage_records(notificationKey) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_notification_usage_linked_usages_notificationKey
                    ON notification_usage_linked_usages(notificationKey)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO notification_usage_linked_usages (notificationKey, moneyUsageId)
                    SELECT notificationKey, moneyUsageId
                    FROM notification_usage_records
                    WHERE moneyUsageId IS NOT NULL
                    """.trimIndent(),
                )
            }
        }
    }
}
