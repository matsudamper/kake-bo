import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import lib.js.NormalizeInputKeyCapture
import net.matsudamper.money.MoneyCompositionLocalProvider
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.rememberMainScreenNavController
import net.matsudamper.money.frontend.common.di.DefaultModule
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.ui.root.Content
import org.jetbrains.compose.resources.configureWebResources
import org.koin.core.context.startKoin
import platform.PlatformToolsProvider

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    configureWebResources {
        resourcePathMapping { path -> "/$path" }
    }
    val koin = startKoin {
        modules(DefaultModule.module)
    }.koin
    val globalEventSender = EventSender<GlobalEvent>()
    ComposeViewport(
        viewportContainerId = "ComposeTargetContainer",
        configure = {
            isA11YEnabled = true
        },
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
                        platformToolsProvider = { PlatformToolsProvider() },
                        navController = navController,
                    )
                }
            }
        }
    }
}
