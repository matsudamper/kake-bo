package net.matsudamper.money.backend.feature.image

object ThumbnailRelativePath {
    private const val SUFFIX = ".thumb.webp"

    /** 元画像の relativePath から拡張子を外して付け替える。`2026-11/{uuid}.jpg` → `2026-11/{uuid}.thumb.webp` */
    fun of(relativePath: String): String {
        return relativePath.substringBeforeLast('.') + SUFFIX
    }
}
