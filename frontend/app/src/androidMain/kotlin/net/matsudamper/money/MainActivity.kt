package net.matsudamper.money

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.nav.user.rememberMainScreenNavController
import net.matsudamper.money.frontend.common.ui.CustomTheme
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.platform.PlatFormToolsImpl
import net.matsudamper.money.ui.root.Content
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val globalEventSender = EventSender<GlobalEvent>()
        setContent {
            MoneyCompositionLocalProvider(
                koin = remember { GlobalContext.get() },
            ) {
                CustomTheme {
                    val navController = rememberMainScreenNavController()
                    BackHandler(navController.canGoBack) {
                        navController.back()
                    }
                    Content(
                        modifier = Modifier.fillMaxSize(),
                        globalEventSender = globalEventSender,
                        platformToolsProvider = { PlatFormToolsImpl(this) },
                        navController = navController,
                    )
                }
            }
        }
    }
}
