package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

public data class AdminLoginScreenUiState(
    val password: TextFieldValue,
    val listener: Listener,
) {
    @Immutable
    public interface Listener {
        public fun onPasswordChanged(text: String)

        public fun onClickLogin()
    }
}

public data class AdminRootScreenUiState(
    val listener: Listener,
) {
    public interface Listener {
        public fun onClickAddUser()

        public fun onClickUnlinkedImages()

        public fun onClickLogout()
    }
}

public data class AdminUnlinkedImagesScreenUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState
        public data class Loaded(
            val items: List<Item>,
            val hasMore: Boolean,
            val isLoadingMore: Boolean,
            val totalCount: Int?,
        ) : LoadingState
    }

    public data class Item(
        val id: String,
        val imageUrl: String,
        val userId: String,
        val userName: String,
    )

    public interface Event {
        public fun onResume()

        public fun onClickRetry()

        public fun onClickLoadMore()
    }
}

public data class AdminAddUserUiState(
    val userName: TextFieldValue,
    val password: TextFieldValue,
    val onChangeUserName: (String) -> Unit,
    val onChangePassword: (String) -> Unit,
    val onClickAddButton: () -> Unit,
)
