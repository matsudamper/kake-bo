package net.matsudamper.money

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDateTime
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.rememberMainScreenNavController
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.platform.PlatFormToolsImpl
import net.matsudamper.money.ui.root.Content
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    private val navControllerFlow: MutableStateFlow<ScreenNavController?> = MutableStateFlow(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val globalEventSender = EventSender<GlobalEvent>()
        val initialStructure = getScreenStructure(intent) ?: RootHomeScreenStructure.Home
        setContent {
            MoneyCompositionLocalProvider(
                koin = remember { GlobalContext.get() },
            ) {
                AppRoot {
                    val navController = rememberMainScreenNavController(initialStructure)
                    LaunchedEffect(navController) {
                        this@MainActivity.navControllerFlow.value = navController
                    }
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

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        lifecycleScope.launch {
            val structure = getScreenStructure(intent) ?: return@launch
            val controller = navControllerFlow.filterNotNull().first()
            controller.navigate(structure)
        }
    }

    private fun getScreenStructure(intent: Intent?): ScreenStructure? {
        if (intent == null) return null
        val uri = intent.data ?: return null
        val path = uri.path ?: return null
        return when (path) {
            "/add/money-usage" -> {
                val queries = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
                val title = queries["title"].orEmpty()
                val price = queries["price"].orEmpty().toFloatOrNull()
                val description = queries["description"].orEmpty()
                val date = run {
                    val dateString = queries["date"]
                    val formatter = DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd")
                        .optionalStart()
                        .appendLiteral(':')
                        .appendPattern("HH-mm-ss")
                        .optionalEnd()
                        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                        .toFormatter()

                    runCatching {
                        LocalDateTime.parse(dateString, formatter)
                    }.onFailure {
                        it.printStackTrace()
                    }.getOrNull()?.toKotlinLocalDateTime()
                }

                ScreenStructure.AddMoneyUsage(
                    title = title,
                    price = price,
                    date = date,
                    description = description,
                )
            }

            else -> null
        }
    }
}
