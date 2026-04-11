package net.matsudamper.money.image

public data class ResizableImageUrl(val url: String) {
    public fun buildUrlWithSize(widthPx: Int, heightPx: Int): String {
        val roundedWidth = roundToNearest100(widthPx)
        val roundedHeight = roundToNearest100(heightPx)
        return "$url?width=$roundedWidth&height=$roundedHeight"
    }

    public companion object {
        public fun roundToNearest100(value: Int): Int {
            return ((value + 50) / 100) * 100
        }
    }
}
