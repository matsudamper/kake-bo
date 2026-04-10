package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NotificationUsageEntity::class],
    version = 2,
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
    }
}
