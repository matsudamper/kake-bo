package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialog
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.NumberInput
import net.matsudamper.money.frontend.common.ui.layout.NumberInputValue
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingScaffold

public data class PresetDetailScreenUiState(
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val loadingState: LoadingState,
    val showNameChangeDialog: FullScreenInputDialog?,
    val numberInputDialog: NumberInputDialog?,
    val showDescriptionChangeDialog: FullScreenInputDialog?,
    val categorySelectDialog: CategorySelectDialogUiState?,
    val event: Event,
) {
    public data class NumberInputDialog(
        val value: NumberInputValue,
        val onChangeValue: (NumberInputValue) -> Unit,
        val dismissRequest: () -> Unit,
    )

    public data class FullScreenInputDialog(
        val defaultText: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onDismiss()

            public fun onCompleted(text: String)
        }
    }

    @Immutable
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState

        public data class Loaded(
            val presetName: String,
            val subCategoryName: String,
            val amount: Int?,
            val description: String?,
        ) : LoadingState
    }

    @Immutable
    public interface Event {
        public fun onResume()
        public fun onClickRetry()

        public fun onClickPresetNameChange()

        public fun onClickSubCategoryChange()

        public fun onClickAmountChange()

        public fun onClickDescriptionChange()

        public fun onClickBack()
    }
}

@Composable
public fun PresetDetailScreen(
    modifier: Modifier = Modifier,
    uiState: PresetDetailScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    uiState.showNameChangeDialog?.let { dialog ->
        FullScreenTextInput(
            title = "プリセット名変更",
            onComplete = { dialog.event.onCompleted(it) },
            canceled = { dialog.event.onDismiss() },
            default = dialog.defaultText,
        )
    }
    uiState.numberInputDialog?.let { dialog ->
        Dialog(onDismissRequest = { dialog.dismissRequest() }) {
            NumberInput(
                value = dialog.value,
                onChangeValue = { dialog.onChangeValue(it) },
                dismissRequest = { dialog.dismissRequest() },
            )
        }
    }
    uiState.showDescriptionChangeDialog?.let { dialog ->
        FullScreenTextInput(
            title = "概要変更",
            onComplete = { dialog.event.onCompleted(it) },
            canceled = { dialog.event.onDismiss() },
            default = dialog.defaultText,
        )
    }
    uiState.categorySelectDialog?.let { dialog ->
        CategorySelectDialog(
            uiState = dialog,
        )
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        SettingScaffold(
            modifier = Modifier.fillMaxSize(),
            title = {
                Text(text = "プリセット詳細")
            },
        ) { paddingValues ->
            when (val state = uiState.loadingState) {
                is PresetDetailScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                PresetDetailScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        onClickRetry = { uiState.event.onClickRetry() },
                    )
                }

                is PresetDetailScreenUiState.LoadingState.Loaded -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    ) {
                        SettingItem(
                            title = "プリセット名",
                            value = state.presetName,
                            onClick = { uiState.event.onClickPresetNameChange() },
                        )
                        SettingItem(
                            title = "サブカテゴリ",
                            value = state.subCategoryName,
                            onClick = { uiState.event.onClickSubCategoryChange() },
                        )
                        SettingItem(
                            title = "金額",
                            value = state.amount?.toString() ?: "未設定",
                            onClick = { uiState.event.onClickAmountChange() },
                        )
                        SettingItem(
                            title = "概要",
                            value = state.description ?: "未設定",
                            onClick = { uiState.event.onClickDescriptionChange() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = value,
                )
            }
            OutlinedButton(
                modifier = Modifier.padding(start = 8.dp),
                onClick = onClick,
            ) {
                Text("変更")
            }
        }
        HorizontalDivider()
    }
}
