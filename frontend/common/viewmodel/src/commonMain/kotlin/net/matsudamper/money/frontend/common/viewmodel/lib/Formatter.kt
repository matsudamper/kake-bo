package net.matsudamper.money.frontend.common.viewmodel.lib

internal object Formatter {
    fun formatMoney(value: Number): String {
        return value.toString().toList()
            .reversed()
            .windowed(3, 3, partialWindows = true)
            .map { it.reversed() }
            .reversed()
            .joinToString(",") { it.joinToString("") }
    }
}
