package net.matsudamper.money.ui.root

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import org.koin.core.Koin

public val LocalKoin: ProvidableCompositionLocal<Koin> = compositionLocalOf {
    error("No Koin provided")
}
