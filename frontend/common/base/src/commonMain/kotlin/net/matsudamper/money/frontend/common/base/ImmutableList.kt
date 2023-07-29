package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable

@Immutable
public class ImmutableList<E>(list: List<E>) : List<E> by list.toList() {
    public companion object {
        public fun <T : List<E>, E> T.toImmutableList(): ImmutableList<E> = ImmutableList(this)
    }
}

public fun <E> immutableListOf(vararg elements: E): ImmutableList<E> = ImmutableList(elements.toList())
