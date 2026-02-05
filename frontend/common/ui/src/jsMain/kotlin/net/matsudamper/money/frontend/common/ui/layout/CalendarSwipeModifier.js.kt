package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.ui.Modifier

public actual fun Modifier.calendarHorizontalSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier = this
