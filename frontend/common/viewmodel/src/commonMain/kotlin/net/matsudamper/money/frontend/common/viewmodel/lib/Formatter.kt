package net.matsudamper.money.frontend.common.viewmodel.lib

import kotlinx.datetime.DayOfWeek

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
}
