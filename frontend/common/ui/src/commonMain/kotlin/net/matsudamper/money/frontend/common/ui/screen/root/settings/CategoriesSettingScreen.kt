package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput

public data class SettingCategoriesScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val showCategoryNameInput: Boolean,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val item: ImmutableList<CategoryItem>,
        ) : LoadingState
    }

    public data class CategoryItem(
        val name: String,
        val color: String?,
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
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    if (uiState.showCategoryNameInput) {
        FullScreenTextInput(
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
                val layoutDirection = LocalLayoutDirection.current
                val lazyListState = rememberLazyListState()
                val fabSize = 56.dp
                val fabPadding = 16.dp
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = paddingValues.calculateStartPadding(layoutDirection),
                            end = paddingValues.calculateEndPadding(layoutDirection),
                            bottom = fabSize + fabPadding * 2,
                        ),
                        state = lazyListState,
                    ) {
                        item {
                            Spacer(Modifier.height(24.dp))
                        }
                        items(state.item) { item ->
                            SettingListMenuItemButton(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                onClick = { item.event.onClick() },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val color = item.color
                                    if (color != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(color)),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = item.name,
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = paddingValues.calculateEndPadding(layoutDirection) + fabPadding,
                                bottom = fabPadding,
                            ),
                        onClick = { uiState.event.onClickAddCategoryButton() },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "カテゴリーを追加")
                    }
                }
            }

            is SettingCategoriesScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    val colorString = hex.removePrefix("#")
    val colorLong = colorString.toLongOrNull(16) ?: return Color.Gray
    return Color(
        red = ((colorLong shr 16) and 0xFF).toInt(),
        green = ((colorLong shr 8) and 0xFF).toInt(),
        blue = (colorLong and 0xFF).toInt(),
    )
}
