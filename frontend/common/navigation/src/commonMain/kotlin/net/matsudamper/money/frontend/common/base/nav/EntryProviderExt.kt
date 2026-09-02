package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure

public inline fun <reified K : T, T : IScreenStructure> EntryProviderScope<T>.addEntryProvider(
    crossinline content: @Composable (current: K) -> Unit,
) {
    addEntryProvider(
        clazz = K::class,
        clazzContentKey = { ContentKeyWrapper(it) },
    ) { current ->
        content(current)
    }
}
