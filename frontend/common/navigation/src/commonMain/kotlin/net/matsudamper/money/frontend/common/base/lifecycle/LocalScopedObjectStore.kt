package net.matsudamper.money.frontend.common.base.lifecycle

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectStoreOwner

public val LocalScopedObjectStore: ProvidableCompositionLocal<ScopedObjectStore> = compositionLocalOf {
    error("No ScopedObjectStore provided")
}

public val LocalScopedObjectStoreOwner: ProvidableCompositionLocal<ScopedObjectStoreOwner> = compositionLocalOf {
    error("No ScopedObjectStoreOwner provided")
}
