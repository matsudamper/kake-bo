package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class SettingCategoriesScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val showCategoryNameInput: Boolean,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val item: ImmutableList<CategoryItem>,
        ) : LoadingState
    }

    public data class CategoryItem(
        val name: String,
        val event: Event,
    ) {
        public interface Event {
            public fun onClick()
        }
    }

    public interface Event {
        public suspend fun onResume()

        public fun onClickAddCategoryButton()

        public fun categoryInputCompleted(text: String)

        public fun dismissCategoryInput()
    }
}

@Composable
public fun SettingCategoriesScreen(
    modifier: Modifier = Modifier,
    uiState: SettingCategoriesScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    if (uiState.showCategoryNameInput) {
        HtmlFullScreenTextInput(
            title = "カテゴリー名",
            onComplete = { text ->
                uiState.event.categoryInputCompleted(text)
            },
            canceled = {
                uiState.event.dismissCategoryInput()
            },
            default = "",
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
public fun MainContent(
    modifier: Modifier,
    uiState: SettingCategoriesScreenUiState,
) {
    SettingScaffold(
        modifier = modifier.fillMaxSize(),
        title = {
            Text(
                text = "カテゴリー一覧",
            )
        },
    ) { paddingValues ->
        when (val state = uiState.loadingState) {
            is SettingCategoriesScreenUiState.LoadingState.Loaded -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    val lazyListState = rememberLazyListState()
                    var listHeightPx by remember { mutableIntStateOf(0) }
                    LazyColumn(
                        modifier =
                            Modifier.fillMaxSize()
                                .onSizeChanged {
                                    listHeightPx = it.height
                                },
                        contentPadding =
                            PaddingValues(
                                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                            ),
                        state = lazyListState,
                    ) {
                        item {
                            Spacer(Modifier.height(24.dp))
                        }
                        stickyHeader {
                            Row {
                                Spacer(modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { uiState.event.onClickAddCategoryButton() },
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text(text = "カテゴリーを追加")
                                }
                            }
                        }
                        items(state.item) { item ->
                            SettingListMenuItemButton(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                                onClick = { item.event.onClick() },
                            ) {
                                Text(
                                    modifier = Modifier,
                                    text = item.name,
                                )
                            }
                        }
                        item {
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    ScrollButtons(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(ScrollButtonsDefaults.padding)
                                .height(ScrollButtonsDefaults.height),
                        scrollState = lazyListState,
                        scrollSize = listHeightPx * 0.4f,
                    )
                }
            }

            is SettingCategoriesScreenUiState.LoadingState.Loading -> {
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
