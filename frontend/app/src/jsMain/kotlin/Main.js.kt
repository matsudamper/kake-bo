import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.flow.MutableStateFlow
import lib.compose.JsCompose
import lib.js.NormalizeInputKeyCapture
import net.matsudamper.money.MoneyCompositionLocalProvider
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.rememberMainScreenNavController
import net.matsudamper.money.frontend.common.di.DefaultModule
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.ui.root.Content
import org.jetbrains.skiko.wasm.onWasmReady
import org.koin.core.context.startKoin
import platform.PlatformToolsProvider

@OptIn(ExperimentalComposeUiApi::class)
fun main(
    @Suppress("UNUSED_PARAMETER") args: Array<String>,
) {
    val koin = startKoin {
        modules(DefaultModule.module)
    }.koin
    val composeSize = MutableStateFlow(IntSize.Zero)
    JsCompose(
        composeSize = composeSize,
    )

    onWasmReady {
        val globalEventSender = EventSender<GlobalEvent>()
        CanvasBasedWindow(
            title = "家計簿",
        ) {
            MoneyCompositionLocalProvider(
                koin = koin,
            ) {
                NormalizeInputKeyCapture {
                    AppRoot {
                        val navController = rememberMainScreenNavController(RootHomeScreenStructure.Home)
                        Content(
                            modifier = Modifier.fillMaxSize(),
                            globalEventSender = globalEventSender,
                            composeSizeProvider = { composeSize },
                            platformToolsProvider = { PlatformToolsProvider() },
                            navController = navController,
                        )
                    }
                }
            }
        }
    }
}
