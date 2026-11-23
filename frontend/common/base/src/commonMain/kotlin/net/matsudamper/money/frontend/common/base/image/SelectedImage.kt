package net.matsudamper.money.frontend.common.base.image

import kotlinx.datetime.LocalDateTime

public data class SelectedImage(
    val id: String,
    val bytes: ByteArray?,
    val contentType: String?,
    /** Web は撮影日時を読めないので、ファイルの最終更新日時が入る */
    val capturedDateTime: LocalDateTime?,
)
