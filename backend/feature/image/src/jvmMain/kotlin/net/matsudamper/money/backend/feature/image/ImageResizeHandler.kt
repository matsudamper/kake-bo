package net.matsudamper.money.backend.feature.image

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

class ImageResizeHandler {
    fun resize(
        file: File,
        contentType: String,
        width: Int,
        height: Int,
    ): Result {
        if (width <= 0 || height <= 0) return Result.InvalidSize
        if (width > MAX_SIZE || height > MAX_SIZE) return Result.InvalidSize

        val originalImage = runCatching { ImageIO.read(file) }.getOrNull()
            ?: return Result.UnsupportedFormat

        val imageType = if (originalImage.colorModel.hasAlpha()) {
            BufferedImage.TYPE_INT_ARGB
        } else {
            BufferedImage.TYPE_INT_RGB
        }
        val resizedImage = BufferedImage(width, height, imageType)
        val g = resizedImage.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.drawImage(originalImage, 0, 0, width, height, null)
        g.dispose()

        val format = when {
            contentType.contains("png") -> "png"
            contentType.contains("gif") -> "gif"
            else -> "jpg"
        }
        val outputContentType = when (format) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }

        val baos = ByteArrayOutputStream()
        val wroteSuccessfully = ImageIO.write(resizedImage, format, baos)
        if (!wroteSuccessfully) return Result.UnsupportedFormat

        return Result.Success(
            data = baos.toByteArray(),
            contentType = outputContentType,
        )
    }

    sealed interface Result {
        data class Success(
            val data: ByteArray,
            val contentType: String,
        ) : Result

        data object InvalidSize : Result
        data object UnsupportedFormat : Result
    }

    private companion object {
        private const val MAX_SIZE = 5000
    }
}
