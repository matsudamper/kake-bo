package net.matsudamper.money.frontend.common.viewmodel

import kotlinx.coroutines.CoroutineScope
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature

public abstract class CommonViewModel(
    private val feature: ScopedObjectFeature,
) {
    public val viewModelScope: CoroutineScope
        get() = feature.coroutineScope
}
