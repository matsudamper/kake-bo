package net.matsudamper.money.frontend.common.ui.layout.html.html

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

public val LocalHtmlRenderContext: ProvidableCompositionLocal<HtmlRenderContext> =
    staticCompositionLocalOf {
        HtmlRenderContext()
    }

@Stable
public class HtmlRenderContext {
    private val _mutableStateFlow: MutableStateFlow<Map<String, RenderState>> = MutableStateFlow(mapOf())
    public val stateFlow: StateFlow<Map<String, RenderState>> = _mutableStateFlow.asStateFlow()

    public fun add(
        id: String,
        html: String,
        onDismissRequest: () -> Unit,
    ) {
        _mutableStateFlow.update {
            it.plus(
                id to
                    RenderState(
                        id = id,
                        html = html,
                        onDismissRequest = onDismissRequest,
                    ),
            )
        }
    }

    public fun remove(id: String) {
        _mutableStateFlow.update {
            it.minus(id)
        }
    }

    public data class RenderState(
        val id: String,
        val html: String,
        val onDismissRequest: () -> Unit,
    )
}
