package net.matsudamper.money.frontend.common.ui.layout.html.text

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public val LocalHtmlTextContext: ProvidableCompositionLocal<HtmlTextContext> = staticCompositionLocalOf {
    HtmlTextContext()
}

public class HtmlTextContext {

    private val _mutableStateFlow: MutableStateFlow<Map<Any, TextState>> = MutableStateFlow(mapOf())
    public val stateFlow: StateFlow<Map<Any, TextState>> = _mutableStateFlow.asStateFlow()
    public fun setPosition(key: Any, positionInRoot: Offset) {
        _mutableStateFlow.update { map ->
            map + (key to map.getOrElse(key) { TextState() }.copy(position = positionInRoot))
        }
    }

    public fun setSize(key: Any, maxWidth: Int? = null, maxHeight: Int? = null) {
        _mutableStateFlow.update { map ->
            map + (key to map.getOrElse(key) { TextState() }.copy(
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            ))
        }
    }

    public fun setSizeCallback(key: Any, callback: (Size) -> Unit) {
        _mutableStateFlow.update { map ->
            map + (key to map.getOrElse(key) { TextState() }.copy(sizeCallback = callback))
        }
    }

    public fun setText(id: Any, text: String) {
        _mutableStateFlow.update { map ->
            map + (id to map.getOrElse(id) { TextState() }.copy(text = text))
        }
    }

    public fun setTextColor(id: Any, color: Color) {
        _mutableStateFlow.update { map ->
            map + (id to map.getOrElse(id) { TextState() }.copy(color = color))
        }
    }

    public fun remove(id: Any) {
        _mutableStateFlow.update { map ->
            map - id
        }
    }

    public data class TextState(
        val position: Offset = Offset.Zero,
        val size: IntSize = IntSize.Zero,
        val text: String = "",
        val color: Color? = null,
        val sizeCallback: (Size) -> Unit = {},
        val maxWidth: Int? = null,
        val maxHeight: Int? = null,
    )
}
