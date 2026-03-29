package net.matsudamper.money.frontend.common.feature.uploader

internal data class ImageUploadEntity(
    val id: String,
    val moneyUsageId: Int,
    val status: String,
    val workManagerId: String?,
    val errorMessage: String?,
    val createdAt: Long,
)
