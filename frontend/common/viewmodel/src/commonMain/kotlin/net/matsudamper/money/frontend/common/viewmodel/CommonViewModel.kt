package net.matsudamper.money.frontend.common.viewmodel

import kotlinx.coroutines.CoroutineScope

public abstract class CommonViewModel(
    private val coroutineScope: CoroutineScope,
) {
    public val viewModelScope: CoroutineScope
        get() = coroutineScope
}
