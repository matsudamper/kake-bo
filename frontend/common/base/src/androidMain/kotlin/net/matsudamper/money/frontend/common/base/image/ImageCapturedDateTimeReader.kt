package net.matsudamper.money.frontend.common.base.image

import android.media.ExifInterface
import java.time.format.DateTimeFormatter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime as JavaLocalDateTime

/**
 * Exif の撮影日時はタイムゾーンを持たない現地時刻なので、変換せずそのまま返す。
 */
public object ImageCapturedDateTimeReader {
    private val exifDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    public fun read(bytes: ByteArray): LocalDateTime? {
        return runCatching {
            val dateTimeOriginal = ExifInterface(bytes.inputStream())
                .getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: return null
            JavaLocalDateTime.parse(dateTimeOriginal, exifDateTimeFormatter).toKotlinLocalDateTime()
        }.getOrNull()
    }
}
