package net.matsudamper.money.frontend.common.viewmodel.lib

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime

internal object Formatter {
    fun formatMoney(value: Number): String {
        return value.toString().toList()
            .reversed()
            .windowed(3, 3, partialWindows = true)
            .map { it.reversed() }
            .reversed()
            .joinToString(",") { it.joinToString("") }
    }

    fun dayOfWeekToJapanese(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "月"
            DayOfWeek.TUESDAY -> "火"
            DayOfWeek.WEDNESDAY -> "水"
            DayOfWeek.THURSDAY -> "木"
            DayOfWeek.FRIDAY -> "金"
            DayOfWeek.SATURDAY -> "土"
            DayOfWeek.SUNDAY -> "日"
        }
    }

    fun formatDateTime(dateTime: LocalDateTime): String {
        return buildString {
            append("${dateTime.year}/${dateTime.monthNumber}/${dateTime.dayOfMonth}")
            append("(${dayOfWeekToJapanese(dateTime.date.dayOfWeek)})")
            append(" ")
            append(dateTime.hour.toString().padStart(2, padChar = '0'))
            append(":")
            append(dateTime.minute.toString().padStart(2, padChar = '0'))
        }
    }
    fun formatDayOfMonthDateTime(dateTime: LocalDateTime): String {
        return buildString {
            append("${dateTime.dayOfMonth}日")
            append("(${dayOfWeekToJapanese(dateTime.date.dayOfWeek)})")
            append(" ")
            append(dateTime.hour.toString().padStart(2, padChar = '0'))
            append(":")
            append(dateTime.minute.toString().padStart(2, padChar = '0'))
        }
    }
}
