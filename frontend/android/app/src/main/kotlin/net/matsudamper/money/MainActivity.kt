package net.matsudamper.money

import android.Manifest
import android.app.ComponentCaller
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDateTime
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.rememberMainScreenNavController
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.platform.PlatFormToolsImpl
import net.matsudamper.money.ui.root.Content
import org.koin.core.context.GlobalContext

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private val navControllerFlow: MutableStateFlow<ScreenNavController?> = MutableStateFlow(null)

    private var notificationPermissionCallback: ((Boolean) -> Unit)? = null

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionCallback?.invoke(granted)
            notificationPermissionCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val globalEventSender = EventSender<GlobalEvent>()
        val platformTools = PlatFormToolsImpl(this) { callback ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionCallback = callback
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                callback(true)
            }
        }
        val initialStructure = getScreenStructure(intent) ?: ScreenStructure.Splash
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
                        platformToolsProvider = { platformTools },
                        navController = navController,
                        onBack = {
                            if (navController.canGoBack) {
                                navController.back()
                            } else {
                                this@MainActivity.onBackPressedDispatcher.onBackPressed()
                            }
                        },
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

                    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

                    runCatching {
                        LocalDateTime.parse(dateString, formatter)
                    }.onFailure {
                        Logger.e(TAG, it)
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
