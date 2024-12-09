package net.matsudamper.money.frontend.common.base.nav

import kotlinx.coroutines.CoroutineScope

public interface ScopedObjectFeature {
    public val coroutineScope: CoroutineScope

    // TODO SavedStateHandle
}
