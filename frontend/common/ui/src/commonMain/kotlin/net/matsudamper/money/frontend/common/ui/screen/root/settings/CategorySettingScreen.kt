package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class SettingCategoryScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val showCategoryNameInput: Boolean,
    val categoryName: String,
    val showCategoryNameChangeDialog: FullScreenInputDialog?,
    val showSubCategoryNameChangeDialog: FullScreenInputDialog?,
) {
    public data class FullScreenInputDialog(
        val initText: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onDismiss()

            public fun onTextInputCompleted(text: String)
        }
    }

    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val item: ImmutableList<SubCategoryItem>,
        ) : LoadingState
    }

    public data class SubCategoryItem(
        val name: String,
        val event: Event,
    ) {
        public interface Event {
            public fun onClick()

            public fun onClickDelete()

            public fun onClickChangeName()
        }
    }

    public interface Event {
        public suspend fun onResume()

        public fun onClickAddSubCategoryButton()

        public fun subCategoryNameInputCompleted(text: String)

        public fun dismissCategoryInput()

        public fun onClickChangeCategoryName()
    }
}

@Composable
public fun SettingCategoryScreen(
    modifier: Modifier = Modifier,
    uiState: SettingCategoryScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    if (uiState.showCategoryNameInput) {
        HtmlFullScreenTextInput(
            title = "サブカテゴリー名",
            onComplete = { text ->
                uiState.event.subCategoryNameInputCompleted(text)
            },
            canceled = {
                uiState.event.dismissCategoryInput()
            },
            default = "",
        )
    }

    if (uiState.showCategoryNameChangeDialog != null) {
        HtmlFullScreenTextInput(
            title = "カテゴリー名変更",
            onComplete = { text ->
                uiState.showCategoryNameChangeDialog.event.onTextInputCompleted(text)
            },
            canceled = {
                uiState.showCategoryNameChangeDialog.event.onDismiss()
            },
            default = uiState.showCategoryNameChangeDialog.initText,
        )
    }
    if (uiState.showSubCategoryNameChangeDialog != null) {
        HtmlFullScreenTextInput(
            title = "サブカテゴリー名変更",
            onComplete = { text ->
                uiState.showSubCategoryNameChangeDialog.event.onTextInputCompleted(text)
            },
            canceled = {
                uiState.showSubCategoryNameChangeDialog.event.onDismiss()
            },
            default = uiState.showSubCategoryNameChangeDialog.initText,
        )
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Settings,
        listener = rootScreenScaffoldListener,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier =
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            rootScreenScaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
        content = {
            MainContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
            )
        },
    )
}

@Composable
public fun MainContent(
    modifier: Modifier,
    uiState: SettingCategoryScreenUiState,
) {
    SettingScaffold(
        modifier = modifier.fillMaxSize(),
        title = {
            Text(
                text = "カテゴリー設定",
            )
        },
    ) { paddingValues ->
        when (val state = uiState.loadingState) {
            is SettingCategoryScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.fillMaxSize(),
                    paddingValues = paddingValues,
                    loadedState = state,
                    uiState = uiState,
                )
            }

            is SettingCategoryScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier =
                    Modifier.fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoadedContent(
    paddingValues: PaddingValues,
    uiState: SettingCategoryScreenUiState,
    loadedState: SettingCategoryScreenUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var scrollButtonHeight by remember { mutableStateOf(0.dp) }
    BoxWithConstraints {
        val containerHeight = maxHeight
        val lazyListState = rememberLazyListState()
        LazyColumn(
            modifier = modifier,
            contentPadding =
            PaddingValues(
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                bottom = scrollButtonHeight,
            ),
            state = lazyListState,
        ) {
            item {
                Spacer(Modifier.height(24.dp))
            }
            item {
                HeaderSection(
                    modifier = Modifier.fillMaxWidth(),
                    categoryName = uiState.categoryName,
                    onClickChangeCategoryNameButton = {
                        uiState.event.onClickChangeCategoryName()
                    },
                    oonClickSubCategoryButton = {
                        uiState.event.onClickAddSubCategoryButton()
                    },
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
            }
            items(
                items = loadedState.item,
            ) { item ->
                SubCategoryItem(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    item = item,
                )
            }
            item {
                Spacer(Modifier.height(24.dp))
            }
        }

        ScrollButtons(
            modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(ScrollButtonsDefaults.padding)
                .height(ScrollButtonsDefaults.height)
                .onSizeChanged {
                    scrollButtonHeight = with(density) { it.height.toDp() }
                },
            scrollState = lazyListState,
            scrollSize = with(density) { containerHeight.toPx() } * 0.4f,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubCategoryItem(
    modifier: Modifier = Modifier,
    item: SettingCategoryScreenUiState.SubCategoryItem,
) {
    Card(
        modifier = modifier,
        onClick = { item.event.onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier =
                Modifier
                    .padding(
                        horizontal = 24.dp,
                        vertical = 24.dp,
                    ),
                text = item.name,
            )
            Spacer(Modifier.weight(1f))
            var showMenu by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    showMenu = true
                },
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "open menu",
                )
            }
            if (showMenu) {
                Popup(
                    alignment = Alignment.CenterEnd,
                    onDismissRequest = {
                        showMenu = false
                    },
                    properties = PopupProperties(focusable = true),
                ) {
                    Card(
                        elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 8.dp,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.width(IntrinsicSize.Min),
                        ) {
                            Text(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { item.event.onClickChangeName() }
                                    .padding(12.dp),
                                text = "名前変更",
                            )
                            Text(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { item.event.onClickDelete() }
                                    .padding(12.dp),
                                text = "削除",
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderSection(
    modifier: Modifier,
    categoryName: String,
    onClickChangeCategoryNameButton: () -> Unit,
    oonClickSubCategoryButton: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowRow {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { onClickChangeCategoryNameButton() },
                ) {
                    Text(text = "カテゴリー名変更")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { oonClickSubCategoryButton() },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(text = "サブカテゴリーを追加")
                }
            }
        }
    }
}
