import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.flow.MutableStateFlow
import lib.compose.JsCompose
import lib.js.NormalizeInputKeyCapture
import net.matsudamper.money.frontend.common.ui.CustomTheme
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main(
    @Suppress("UNUSED_PARAMETER") args: Array<String>,
) {
    val composeSize = MutableStateFlow(IntSize.Zero)
    JsCompose(
        composeSize = composeSize,
    )

    onWasmReady {
        val globalEventSender = EventSender<GlobalEvent>()
        CanvasBasedWindow(
            title = "家計簿",
        ) {
            NormalizeInputKeyCapture {
                CustomTheme {
                    Content(
                        modifier = Modifier.fillMaxSize(),
                        globalEventSender = globalEventSender,
                        composeSizeProvider = { composeSize },
                    )
                }
            }
        }
    }
}
