package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput

public data class PresetListScreenUiState(
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val loadingState: LoadingState,
    val showNameInput: Boolean,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState
        public data class Loaded(
            val items: ImmutableList<PresetItem>,
        ) : LoadingState
    }

    public data class PresetItem(
        val name: String,
        val subCategoryName: String?,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClick()

            public fun onClickEdit()

            public fun onClickDelete()
        }
    }

    @Immutable
    public interface Event {
        public fun onResume()

        public fun onClickAddButton()

        public fun onNameInputCompleted(name: String)

        public fun onDismissNameInput()
    }
}

@Composable
public fun PresetListScreen(
    modifier: Modifier = Modifier,
    uiState: PresetListScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    if (uiState.showNameInput) {
        FullScreenTextInput(
            title = "プリセット名",
            onComplete = { text ->
                uiState.event.onNameInputCompleted(text)
            },
            canceled = {
                uiState.event.onDismissNameInput()
            },
            default = "",
        )
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
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
        when (val state = uiState.loadingState) {
            is PresetListScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is PresetListScreenUiState.LoadingState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("データの取得に失敗しました")
                        Text(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { uiState.event.onResume() },
                            text = "再試行",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            is PresetListScreenUiState.LoadingState.Loaded -> {
                val layoutDirection = LocalLayoutDirection.current
                val fabSize = 56.dp
                val fabPadding = 16.dp
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = fabSize + fabPadding * 2),
                    ) {
                        items(state.items) { item ->
                            var showMenu by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp,
                                    ),
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { item.event.onClick() },
                                                onLongPress = { showMenu = true },
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                val pressEvent = awaitPointerEvent()
                                                if (pressEvent.type != PointerEventType.Press) return@awaitEachGesture
                                                if (pressEvent.buttons.isSecondaryPressed.not()) return@awaitEachGesture
                                                while (true) {
                                                    val releaseEvent = awaitPointerEvent()
                                                    if (releaseEvent.type != PointerEventType.Release) continue
                                                    showMenu = true
                                                    break
                                                }
                                            }
                                        },
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 16.dp,
                                                vertical = 12.dp,
                                            ),
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            modifier = Modifier.padding(top = 4.dp),
                                            text = item.subCategoryName ?: "サブカテゴリ未設定",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("編集") },
                                        onClick = {
                                            showMenu = false
                                            item.event.onClickEdit()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("削除") },
                                        onClick = {
                                            showMenu = false
                                            item.event.onClickDelete()
                                        },
                                    )
                                }
                            }
                        }
                    }
                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = windowInsets.calculateEndPadding(layoutDirection) + fabPadding,
                                bottom = fabPadding,
                            ),
                        onClick = { uiState.event.onClickAddButton() },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "プリセットを追加")
                    }
                }
            }
        }
    }
}
