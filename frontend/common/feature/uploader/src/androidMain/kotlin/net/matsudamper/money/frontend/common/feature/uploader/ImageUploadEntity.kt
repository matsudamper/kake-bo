package net.matsudamper.money.frontend.common.feature.uploader

internal data class ImageUploadEntity(
    val id: String,
    val moneyUsageId: Int,
    val rawImageBytes: ByteArray,
    val previewBytes: ByteArray?,
    val status: String,
    val workManagerId: String?,
    val errorMessage: String?,
    val createdAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageUploadEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
