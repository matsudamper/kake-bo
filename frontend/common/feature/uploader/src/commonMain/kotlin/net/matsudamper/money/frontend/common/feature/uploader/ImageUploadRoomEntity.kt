package net.matsudamper.money.frontend.common.feature.uploader

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Suppress("ArrayInDataClass")
@Entity(tableName = "image_upload_queue")
internal data class ImageUploadRoomEntity(
    @PrimaryKey val id: String,
    val moneyUsageId: Int,
    val status: String,
    val errorMessage: String?,
    val stackTrace: String?,
    val contentType: String?,
    val createdAt: Long,
    val workManagerId: String?,
    val rawImageBytes: ByteArray?,
    val previewBytes: ByteArray?,
)
