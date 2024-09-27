package net.matsudamper.money.frontend.common.viewmodel

import kotlinx.coroutines.CoroutineScope

public interface ViewModelFeature {
    public val coroutineScope: CoroutineScope

    // TODO SavedStateHandle
}
