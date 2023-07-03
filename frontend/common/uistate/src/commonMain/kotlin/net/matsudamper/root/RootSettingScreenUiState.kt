package net.matsudamper.root

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.base.ImmutableList

public data class RootSettingScreenUiState(
    val textInputEvents: ImmutableList<TextInputUiState>,
    val loadingState: LoadingState,
    val event: Event,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public data class Loaded(
            val imapConfig: ImapConfig,
        ) : LoadingState
    }

    public data class ImapConfig(
        val host: String,
        val userName: String,
        val port: String,
        val password: String,
        val event: Event,
    ) {
        public interface Event {
            public fun onClickChangeHost()
            public fun onClickChangeUserName()
            public fun onClickChangePort()
            public fun onClickChangePassword()
        }
    }

    @Immutable
    public class TextInputUiState(
        public val title: String,
        public val default: String,
        public val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun complete(text: String, event: TextInputUiState)
            public fun cancel(event: TextInputUiState)
        }
    }

    public interface Event {
        public fun consumeTextInputEvent(event: TextInputUiState)
        public fun onResume()
    }
}
