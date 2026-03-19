package net.matsudamper.money.frontend.common.base.platform


public interface ImagePicker {
    public suspend fun pickImages(): List<SelectedImage>

    public data class SelectedImage(
        val id: String,
        val previewBytes: ByteArray?,
        val await: suspend () -> UploadedImageData?,
    )

    public data class UploadedImageData(
        val bytes: ByteArray,
        val contentType: String?,
    )
}
