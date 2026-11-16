package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.CalendarLocale

/** DatePicker の週の始まりは地域で決まるため、日曜始まりの日本を指定する */
internal expect fun sundayStartCalendarLocale(): CalendarLocale
