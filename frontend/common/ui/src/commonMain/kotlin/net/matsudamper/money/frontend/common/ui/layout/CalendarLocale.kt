package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.CalendarLocale

/** DatePicker の週の始まりは地域で決まるため、地域が省略されていれば補ったロケールを返す */
internal expect fun currentCalendarLocaleWithRegion(): CalendarLocale
