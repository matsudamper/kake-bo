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
    val screenState: ScreenState,
    val event: Event,
) {
    public sealed interface ScreenState {
        public data object Loading : ScreenState
        public data object Error : ScreenState
        public data class MonthList(
            val months: List<MonthItem>,
        ) : ScreenState

        public data class MonthDetail(
            val yearMonth: String,
            val items: List<Item>,
            val selectedIds: Set<String>,
            val isDeleting: Boolean,
        ) : ScreenState
    }

    public data class MonthItem(
        val yearMonth: String,
        val count: Int,
    )

    public data class Item(
        val id: String,
        val imageUrl: String,
        val userId: String,
        val userName: String,
    )

    public interface Event {
        public fun onResume()

        public fun onClickRetry()

        public fun onClickMonth(yearMonth: String)

        public fun onClickBack()

        public fun onToggleImageSelection(id: String)

        public fun onClickSelectAll()

        public fun onClickDeselectAll()

        public fun onClickDeleteSelected()
    }
}

public data class AdminAddUserUiState(
    val userName: TextFieldValue,
    val password: TextFieldValue,
    val onChangeUserName: (String) -> Unit,
    val onChangePassword: (String) -> Unit,
    val onClickAddButton: () -> Unit,
)
