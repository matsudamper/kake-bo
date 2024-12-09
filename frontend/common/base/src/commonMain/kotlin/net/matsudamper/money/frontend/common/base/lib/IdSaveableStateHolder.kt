package net.matsudamper.money.frontend.common.base.lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

/**
 * 元のrememberSaveableStateHolderは`LocalSaveableStateRegistry.current`を使用している。
 * 同じ場所でrememberSaveableStateHolderを使用すると同じものを使用しているのとおなじになる。
 * 別々になるように、idをkeyのprefixをするようにする。
 */
@Composable
public fun rememberSaveableStateHolder(id: Any): SaveableStateHolder {
    val holder = rememberSaveableStateHolder()
    return remember(id) {
        IdSaveableStateHolderImpl(holder, id)
    }
}

private class IdSaveableStateHolderImpl(
    private val holder: SaveableStateHolder,
    private val id: Any,
) : SaveableStateHolder {
    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        // TODO: AndroidではSerializableにしないといけないので、とりあえずStringにしておく
        holder.SaveableStateProvider(key = IdSaveableStateKey(id, key).toString(), content = content)
    }

    override fun removeState(key: Any) {
        holder.removeState(IdSaveableStateKey(id, key).toString())
    }

    private data class IdSaveableStateKey(
        val id: Any,
        val key: Any,
    )
}
