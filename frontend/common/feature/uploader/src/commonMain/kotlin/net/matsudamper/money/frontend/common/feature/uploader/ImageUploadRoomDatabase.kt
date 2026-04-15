package net.matsudamper.money.frontend.common.feature.uploader

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(entities = [ImageUploadRoomEntity::class], version = 4, exportSchema = false)
@ConstructedBy(ImageUploadDatabaseConstructor::class)
internal abstract class ImageUploadRoomDatabase : RoomDatabase() {
    internal abstract fun dao(): ImageUploadRoomDao

    internal companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE image_upload_queue ADD COLUMN stackTrace TEXT")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE image_upload_queue ADD COLUMN contentType TEXT")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                // MIGRATION_1_2を経由せずにVersion 3になったDBにはstackTraceカラムが存在しない可能性があるため、
                // 追加を試みてすでに存在する場合のエラーは無視する
                try {
                    connection.execSQL("ALTER TABLE image_upload_queue ADD COLUMN stackTrace TEXT")
                } catch (_: Exception) {
                    // stackTraceカラムが既に存在する場合は無視
                }

                connection.execSQL(
                    """CREATE TABLE image_upload_queue_new (
                        |id TEXT NOT NULL PRIMARY KEY,
                        |moneyUsageId INTEGER NOT NULL,
                        |status TEXT NOT NULL,
                        |errorMessage TEXT,
                        |stackTrace TEXT,
                        |contentType TEXT,
                        |createdAt INTEGER NOT NULL,
                        |workManagerId TEXT
                        |)
                    """.trimMargin(),
                )
                connection.execSQL(
                    """INSERT INTO image_upload_queue_new
                        |(id, moneyUsageId, status, errorMessage, stackTrace, contentType, createdAt, workManagerId)
                        |SELECT id, moneyUsageId, status, errorMessage, stackTrace, contentType, createdAt, workManagerId
                        |FROM image_upload_queue
                    """.trimMargin(),
                )
                connection.execSQL("DROP TABLE image_upload_queue")
                connection.execSQL("ALTER TABLE image_upload_queue_new RENAME TO image_upload_queue")
            }
        }
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object ImageUploadDatabaseConstructor :
    RoomDatabaseConstructor<ImageUploadRoomDatabase> {
    override fun initialize(): ImageUploadRoomDatabase
}
