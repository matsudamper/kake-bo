package net.matsudamper.money.backend.mail.parser.lib

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.Locale

internal object ParseUtil {
    fun getInt(value: String): Int? {
        return value
            .mapNotNull { it.toString().toIntOrNull() }
            .joinToString("")
            .toIntOrNull()
    }

    fun normalizeHalfWidthKatakana(value: String): String {
        val builder = StringBuilder()
        var index = 0
        while (index < value.length) {
            val start = index
            while (index < value.length && value[index].code in 0xFF61..0xFF9F) {
                index++
            }
            if (index > start) {
                builder.append(
                    Normalizer.normalize(value.substring(start, index), Normalizer.Form.NFKC),
                )
            } else {
                builder.append(value[index])
                index++
            }
        }
        return builder.toString()
    }

    fun removeHtmlTag(value: String): String {
        return "<[^<>\n]+>".toRegex().replace(value, "")
    }

    /**
     * `prefix(.+?)suffix` の正規表現と同じ結果を返す。
     * 正規表現では prefix を繰り返した行で開始位置ごとに行末まで再走査し、処理時間が入力長の2乗になる。
     */
    fun findBetween(text: String, prefix: String, suffix: String): String? {
        var searchStart = 0
        while (true) {
            val prefixIndex = text.indexOf(prefix, searchStart).takeIf { it >= 0 } ?: return null
            val lineEnd = text.indexOfAny(charArrayOf('\n', '\r'), prefixIndex).takeIf { it >= 0 } ?: text.length
            val valueStart = prefixIndex + prefix.length
            val valueLength = text.substring(valueStart, lineEnd).indexOf(suffix, startIndex = 1)
            if (valueLength >= 0) return text.substring(valueStart, valueStart + valueLength)
            searchStart = lineEnd
        }
    }

    fun splitByNewLine(value: String): List<String> {
        return value.split("\r\n")
            .flatMap { it.split("\n") }
    }

    fun parseForwarded(text: String): MailMetadata? {
        val lines = splitByNewLine(text)

        val forwardedStartIndex = lines.indexOf("---------- Forwarded message ---------")
            .takeIf { it >= 0 } ?: return null

        val relativeForwardedEndIndex = lines.subList(forwardedStartIndex, lines.size)
            .indexOf("")
            .takeIf { it >= 0 } ?: return null
        val forwardedEndIndex = forwardedStartIndex + relativeForwardedEndIndex

        val forwardedMetadata = lines.subList(forwardedStartIndex, forwardedEndIndex)
            .associate {
                val split = it.split(":")
                split.first() to split.drop(1).joinToString(":")
            }

        return MailMetadata(
            from = forwardedMetadata["From"]?.trim()?.let from@{ fromRawString ->
                val result = "<([^<>]+)>".toRegex().findAll(fromRawString).lastOrNull()
                    ?: return@from null

                result.groupValues[1]
            },
            fromPersonal = forwardedMetadata["From"]?.trim()?.let from@{ fromRawString ->
                val result = "^(.+)<".toRegex().findAll(fromRawString).lastOrNull()
                    ?: return@from null

                result.groupValues[1]
            },
            date = forwardedMetadata["Date"]?.trim()?.let { dateRawString ->
                val result = runCatching {
                    forwardedMailDateJapaneseFormatter.parse(dateRawString)
                }.onFailure {
                    it.printStackTrace()
                }.getOrNull() ?: return@let null

                LocalDateTime.of(
                    LocalDate.of(
                        result.get(ChronoField.YEAR),
                        result.get(ChronoField.MONTH_OF_YEAR),
                        result.get(ChronoField.DAY_OF_MONTH),
                    ),
                    LocalTime.of(
                        result.get(ChronoField.HOUR_OF_DAY),
                        result.get(ChronoField.MINUTE_OF_HOUR),
                    ),
                )
            },
            subject = forwardedMetadata["Subject"]?.trim(),
            to = forwardedMetadata["To"]?.trim()?.let to@{ toRawString ->
                val result = "<(.+?)>".toRegex().findAll(toRawString).lastOrNull()
                    ?: return@to null

                result.groupValues[1]
            },
        )
    }

    data class MailMetadata(
        val from: String?,
        val fromPersonal: String?,
        val date: LocalDateTime?,
        val subject: String?,
        val to: String?,
    )

    private val forwardedMailDateJapaneseFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendLiteral('年')
        .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NEVER)
        .appendLiteral("月")
        .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NEVER)
        .appendLiteral("日")
        .appendPattern("(E) ")
        .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NEVER)
        .appendLiteral(":")
        .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NEVER)
        .toFormatter()
        .withLocale(Locale.JAPANESE)
}
