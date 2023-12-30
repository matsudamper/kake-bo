package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class SettingMailCategoryFilterScreenUiState(
    public val event: Event,
    public val textInput: TextInput?,
    public val loadingState: LoadingState,
) {
    public data class TextInput(
        val title: String,
        val onCompleted: (String) -> Unit,
        val dismiss: () -> Unit,
    )

    public sealed interface LoadingState {

        public data object Loading : LoadingState
        public data object Error : LoadingState
        public data class Loaded(
            val filters: ImmutableList<Item>,
            val isError: Boolean,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class Item(
        val title: String,
        val event: ItemEvent,
    )

    @Immutable
    public interface LoadedEvent {
        public fun onClickAdd()
    }

    @Immutable
    public interface ItemEvent {
        public fun onClick()
    }

    @Immutable
    public interface Event {
        public fun onClickRetry()
        public fun onViewInitialized()
        public fun onClickBack()
    }
}

@Composable
public fun SettingMailCategoryFiltersScreen(
    modifier: Modifier = Modifier,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    uiState: SettingMailCategoryFilterScreenUiState,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    uiState.textInput?.also { textInput ->
        HtmlFullScreenTextInput(
            title = textInput.title,
            onComplete = { textInput.onCompleted(it) },
            canceled = { textInput.dismiss() },
            default = "",
            isMultiline = false,
        )
    }
    RootScreenScaffold(
        modifier = modifier,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            rootScreenScaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        currentScreen = RootScreenTab.Settings,
        listener = rootScreenScaffoldListener,
    ) {
        SettingScaffold(
            title = {
                Text("メールカテゴリフィルタ一覧")
            },
        ) { paddingValues ->
            when (val loadingState = uiState.loadingState) {
                is SettingMailCategoryFilterScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                is SettingMailCategoryFilterScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = loadingState,
                        contentPadding = paddingValues,
                    )
                }

                SettingMailCategoryFilterScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        onClickRetry = uiState.event::onClickRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: SettingMailCategoryFilterScreenUiState.LoadingState.Loaded,
    contentPadding: PaddingValues,
) {
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()
    BoxWithConstraints(modifier = modifier) {
        val height by rememberUpdatedState(maxHeight)
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(contentPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { uiState.event.onClickAdd() }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("追加")
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(8.dp),
            ) {
                items(uiState.filters) { item ->
                    SettingListMenuItemButton(onClick = { item.event.onClick() }) {
                        Text(item.title)
                    }
                }
            }
        }
        ScrollButtons(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(ScrollButtonsDefaults.padding)
                .height(ScrollButtonsDefaults.height),
            scrollState = lazyListState,
            scrollSize = with(density) { height.toPx() } * 0.4f,
        )
    }
}
