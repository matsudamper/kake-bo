package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.ui.Modifier

public expect fun Modifier.calendarHorizontalSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier
