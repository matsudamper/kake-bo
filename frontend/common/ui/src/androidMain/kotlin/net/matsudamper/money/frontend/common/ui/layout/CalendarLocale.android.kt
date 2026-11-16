package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.CalendarLocale
import java.util.Locale

internal actual fun currentCalendarLocaleWithRegion(): CalendarLocale = Locale.getDefault()
