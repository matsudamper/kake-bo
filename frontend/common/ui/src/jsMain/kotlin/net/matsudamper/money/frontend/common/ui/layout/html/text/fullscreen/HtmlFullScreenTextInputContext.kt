package net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public val LocalHtmlFullScreenTextInputContext: ProvidableCompositionLocal<HtmlFullScreenTextInputContext> = staticCompositionLocalOf {
    HtmlFullScreenTextInputContext()
}

public class HtmlFullScreenTextInputContext {
    private val mutableStateFlow: MutableStateFlow<Map<String, TextState>> = MutableStateFlow(mapOf())
    public val stateFlow: StateFlow<Map<String, TextState>> = mutableStateFlow.asStateFlow()

    public fun set(
        id: String,
        textState: TextState,
    ) {
        mutableStateFlow.update { map ->
            map + (id to map.getOrElse(id) { textState })
        }
    }

    public fun remove(id: String) {
        mutableStateFlow.update { map ->
            map - id
        }
    }

    public data class TextState(
        val title: String,
        val name: String,
        val textComplete: (String) -> Unit,
        val canceled: () -> Unit,
        val default: String,
        val isMultiline: Boolean,
        val inputType: String,
    )
}
