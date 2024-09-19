package net.matsudamper.money.ui.root

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import net.matsudamper.money.frontend.common.feature.webauth.DefaultModule
import org.koin.core.Koin
import org.koin.core.context.startKoin

val LocalKoin: ProvidableCompositionLocal<Koin> = compositionLocalOf {
    error("No Koin provided")
}
