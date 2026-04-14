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

@Suppress("KotlinNoActualForExpect")
internal expect object ImageUploadDatabaseConstructor :
    RoomDatabaseConstructor<ImageUploadRoomDatabase> {
    override fun initialize(): ImageUploadRoomDatabase
}
