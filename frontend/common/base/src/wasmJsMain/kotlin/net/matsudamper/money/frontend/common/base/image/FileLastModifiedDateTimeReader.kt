package net.matsudamper.money.frontend.common.base.image

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.w3c.files.File

public object FileLastModifiedDateTimeReader {
    public fun read(file: File): LocalDateTime {
        return Instant.fromEpochMilliseconds(lastModifiedEpochMillis(file).toLong())
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }
}

// File.lastModified は Int で宣言されていてエポックミリ秒が収まらない
private fun lastModifiedEpochMillis(file: File): Double = js("file.lastModified")
