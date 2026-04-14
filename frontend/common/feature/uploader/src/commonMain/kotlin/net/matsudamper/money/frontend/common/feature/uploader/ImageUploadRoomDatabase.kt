package net.matsudamper.money.frontend.common.feature.uploader

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [ImageUploadRoomEntity::class], version = 1, exportSchema = false)
@ConstructedBy(ImageUploadDatabaseConstructor::class)
internal abstract class ImageUploadRoomDatabase : RoomDatabase() {
    internal abstract fun dao(): ImageUploadRoomDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object ImageUploadDatabaseConstructor :
    RoomDatabaseConstructor<ImageUploadRoomDatabase> {
    override fun initialize(): ImageUploadRoomDatabase
}
