package net.matsudamper.money.frontend.common.base.lifecycle

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectStore

public val LocalScopedObjectStore: ProvidableCompositionLocal<ScopedObjectStore> = compositionLocalOf<ScopedObjectStore> {
    error("No ScopedObjectStore provided")
}
