package net.matsudamper.money.frontend.common.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalIsLargeScreen: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf<Boolean> { error("LocalIsLargeScreen") }
