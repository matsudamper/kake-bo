package net.matsudamper.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import net.matsudamper.money.ui.root.LocalKoin
import org.koin.core.Koin

@Composable
internal fun MoneyCompositionLocalProvider(
    koin: Koin,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalKoin provides koin,
        content = content,
    )
}
