package net.matsudamper.money.backend.mail.parser.lib

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.Locale
import net.matsudamper.money.backend.base.TraceLogger

internal object ParseUtil {
    fun getInt(value: String): Int? {
        return value
            .mapNotNull { it.toString().toIntOrNull() }
            .joinToString("")
            .toIntOrNull()
    }

    fun removeHtmlTag(value: String): String {
        return "<.+?>".toRegex().replace(value, "")
    }

    fun splitByNewLine(value: String): List<String> {
        return value.split("\r\n")
            .flatMap { it.split("\n") }
    }

    fun parseForwarded(text: String): MailMetadata? {
        val lines = splitByNewLine(text)

        val forwardedStartIndex = lines.indexOf("---------- Forwarded message ---------")
            .takeIf { it >= 0 } ?: return null

        val forwardedEndIndex = lines.subList(forwardedStartIndex, lines.size)
            .indexOf("")
            .takeIf { it >= 0 } ?: return null

        val forwardedMetadata = lines.subList(forwardedStartIndex, forwardedEndIndex)
            .associate {
                val split = it.split(":")
                split.first() to split.drop(1).joinToString(":")
            }

        return MailMetadata(
            from = forwardedMetadata["From"]?.trim()?.let from@{ fromRawString ->
                val result = "<(.+?)>".toRegex().findAll(fromRawString).lastOrNull()
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
                    TraceLogger.impl().noticeThrowable(it, mapOf(), true)
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
