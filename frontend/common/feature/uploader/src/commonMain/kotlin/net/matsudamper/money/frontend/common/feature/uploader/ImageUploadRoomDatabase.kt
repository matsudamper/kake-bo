package net.matsudamper.money.frontend.common.feature.uploader

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(entities = [ImageUploadRoomEntity::class], version = 2, exportSchema = false)
@ConstructedBy(ImageUploadDatabaseConstructor::class)
internal abstract class ImageUploadRoomDatabase : RoomDatabase() {
    internal abstract fun dao(): ImageUploadRoomDao

    internal companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE image_upload_queue ADD COLUMN stackTrace TEXT")
            }
        }
    }
}

@Suppress("KotlinNoActualForExpect")
internal expect object ImageUploadDatabaseConstructor :
    RoomDatabaseConstructor<ImageUploadRoomDatabase> {
    override fun initialize(): ImageUploadRoomDatabase
}
