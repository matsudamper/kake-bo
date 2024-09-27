package net.matsudamper.money.frontend.common.viewmodel

import kotlinx.coroutines.CoroutineScope

public abstract class CommonViewModel(
    private val feature: ViewModelFeature,
) {
    public val viewModelScope: CoroutineScope
        get() = feature.coroutineScope
}
