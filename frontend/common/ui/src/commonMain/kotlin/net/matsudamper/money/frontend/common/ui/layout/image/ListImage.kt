package net.matsudamper.money.frontend.common.ui.layout.image

/**
 * 一覧のタイルにはサムネイルを出し、タップしたら元画像を開く。
 * サムネイル未生成の画像は両方に元画像の URL が入る
 */
public data class ListImage(
    val thumbnailUrl: String,
    val url: String,
)
