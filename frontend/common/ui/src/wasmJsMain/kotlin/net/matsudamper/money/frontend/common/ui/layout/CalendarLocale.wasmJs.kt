package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.CalendarLocale
import androidx.compose.ui.text.intl.Locale

internal actual fun currentCalendarLocaleWithRegion(): CalendarLocale {
    return Locale(maximizeLanguageTag(Locale.current.toLanguageTag()))
}

private fun maximizeLanguageTag(languageTag: String): String = js("new Intl.Locale(languageTag).maximize().toString()")
