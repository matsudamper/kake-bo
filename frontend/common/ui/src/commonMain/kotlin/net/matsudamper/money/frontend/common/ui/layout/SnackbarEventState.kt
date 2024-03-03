package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Stable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@Stable
public class SnackbarEventState {
    private val state: Channel<suspend (Bridge) -> Unit> = Channel(Channel.UNLIMITED)

    public suspend fun show(event: Event): Result {
        var result: Result = Result.Dismiss
        state.send {
            result = it.call(event)
        }
        return result
    }

    public suspend fun collect(block: suspend (Event) -> Result) {
        state.receiveAsFlow().collect { bridge ->
            bridge(
                object : Bridge {
                    override suspend fun call(event: Event): Result {
                        return block(event)
                    }
                },
            )
        }
    }

    public interface Bridge {
        public suspend fun call(event: Event): Result
    }

    public data class Event(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration? = null,
    )

    public enum class Result {
        Dismiss,
        Action,
    }
}
