package net.matsudamper.money.frontend.common.viewmodel.root

import androidx.compose.animation.core.spring
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import RootHomeScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.ScreenNavController
import net.matsudamper.money.frontend.graphql.GraphqlMailQuery

// TODO move
public class EventSender<Receiver> {
    private val receiverChannel = Channel<suspend (Receiver) -> Unit>(Channel.UNLIMITED)
    public suspend fun <R> send(block: suspend (Receiver) -> R): R {
        val scope = CoroutineScope(coroutineContext)
        return suspendCoroutine { continuation ->
            scope.launch {
                receiverChannel.send { receiver ->
                    continuation.resume(block(receiver))
                }
            }
        }
    }

    public fun asReceiver(): EventReceiver<Receiver> {
        return EventReceiver(receiverChannel)
    }
}

public class EventReceiver<Receiver>(
    private val events: ReceiveChannel<suspend (Receiver) -> Unit>,
) {
    public suspend fun collect(target: Receiver) {
        events.receiveAsFlow().collect { block ->
            block(target)
        }
    }
}

public interface GlobalEvent {
    public fun showSnackBar(message: String)
    public fun showNativeNotification(message: String)
}

public class HomeViewModel(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val navController: ScreenNavController,
    private val mailQuery: GraphqlMailQuery,
    private val globalEventSender: EventSender<GlobalEvent>,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val rootUiStateFlow: StateFlow<RootHomeScreenUiState> = MutableStateFlow(
        RootHomeScreenUiState(
            isLoading = true,
        ),
    ).also {
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                it.update {
                    it.copy(
                        isLoading = viewModelState.isLoading,
                    )
                }
            }
        }
    }.asStateFlow()

    public fun onResume() {
        coroutineScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    graphqlQuery.isLoggedIn()
                }
            }.onFailure { e ->
                globalEventSender.send {
                    it.showSnackBar("${e.message}")
                }
            }.onSuccess { isLoggedIn ->
                if (isLoggedIn.not()) {
                    navController.navigate(Screen.Login)
                } else {
                    viewModelStateFlow.update {
                        it.copy(
                            isLoading = false,
                        )
                    }
                }
            }

            val mails = runCatching {
                withContext(ioDispatcher){
                    mailQuery.getMail()
                }
            }.getOrNull() ?: return@launch

            println("usrMails: ${mails.data?.user?.mail?.usrMails.toString()}")
            mails.data?.user?.mail?.usrMails?.onEach {
                println("mail: $it")
            }
        }
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
    )
}
