package net.matsudamper.money.frontend.common.viewmodel.lib

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number

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

    fun formatTime(time: LocalTime): String {
        return buildString {
            append(time.hour.toString().padStart(2, padChar = '0'))
            append(":")
            append(time.minute.toString().padStart(2, padChar = '0'))
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

    fun formatYearMonthDateTime(dateTime: LocalDateTime): String {
        return buildString {
            append(dateTime.year.toString().padStart(4, padChar = '0'))
            append("/")
            append(dateTime.monthNumber.toString().padStart(2, padChar = '0'))
            append("/")
            append(dateTime.dayOfMonth.toString().padStart(2, padChar = '0'))
            append("・")
            append(dateTime.hour.toString().padStart(2, padChar = '0'))
            append(":")
            append(dateTime.minute.toString().padStart(2, padChar = '0'))
        }
    }

    fun formatDate(date: LocalDateTime): String {
        return buildString {
            append("${date.year}/${date.month.number}/${date.day}")
            append("(${dayOfWeekToJapanese(date.date.dayOfWeek)})")
        }
    }
}
