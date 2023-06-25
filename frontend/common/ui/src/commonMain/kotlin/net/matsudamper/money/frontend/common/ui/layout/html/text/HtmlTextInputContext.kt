package net.matsudamper.money.frontend.common.ui.layout.html.text

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


public val LocalHtmlTextInputContext: ProvidableCompositionLocal<HtmlTextInputContext> = staticCompositionLocalOf {
    HtmlTextInputContext()
}

public class HtmlTextInputContext {

    private val _mutableStateFlow: MutableStateFlow<Map<Any, TextState>> = MutableStateFlow(mapOf())
    public val stateFlow: StateFlow<Map<Any, TextState>> = _mutableStateFlow.asStateFlow()
    public fun setPosition(key: Any, positionInRoot: Offset) {
        _mutableStateFlow.update { map ->
            map + (key to map.getOrElse(key) { TextState() }.copy(position = positionInRoot))
        }
    }

    public fun setSize(key: Any, width: Int? = null, maxHeight: Int? = null) {
        _mutableStateFlow.update { map ->
            map + (key to map.getOrElse(key) { TextState() }.copy(
                width = width,
                maxHeight = maxHeight,
            ))
        }
    }

    public fun setSizeCallback(key: Any, callback: (Size) -> Unit) {
        _mutableStateFlow.update { map ->
            map + (key to map.getOrElse(key) { TextState() }.copy(sizeCallback = callback))
        }
    }

    public fun setColor(key: Any, color: Color) {
        _mutableStateFlow.update { map ->
            map + (key to map.getOrElse(key) { TextState() }.copy(color = color))
        }
    }

    public fun setTextCallback(key: Any, callback: (String) -> Unit) {
        _mutableStateFlow.update { map ->
            map + (key to map.getOrElse(key) { TextState() }.copy(textCallback = callback))
        }
    }

    public fun setPlaceHolder(id: Any, placeholder: String) {
        _mutableStateFlow.update { map ->
            map + (id to map.getOrElse(id) { TextState() }.copy(placeholder = placeholder))
        }
    }

    public fun remove(id: Any) {
        _mutableStateFlow.update { map ->
            map - id
        }
    }

    public fun setType(id: Any, type: KeyboardType) {
        _mutableStateFlow.update { map ->
            map + (id to map.getOrElse(id) { TextState() }.copy(
                type = when (type) {
                    KeyboardType.Text -> "text"
                    KeyboardType.Password -> "password"
                    KeyboardType.Number -> "number"
                    else -> TODO()
                },
            ))
        }
    }

    public data class TextState(
        val position: Offset = Offset.Zero,
        val width: Int? = null,
        val maxHeight: Int? = null,
        val placeholder: String = "",
        val color: Color? = null,
        val type: String? = null,
        val sizeCallback: (Size) -> Unit = { println("default: sizeCallback") },
        val textCallback: (String) -> Unit = { println("default: TextCallback") },
    )
}
