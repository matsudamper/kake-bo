package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

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

        public object Loading : LoadingState
        public object Error : LoadingState
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
    }
}

@Composable
public fun SettingMailCategoryFilterScreen(
    modifier: Modifier = Modifier,
    listener: RootScreenScaffoldListener,
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
        currentScreen = RootScreenTab.Settings,
        listener = listener,
    ) {
        SettingScaffold(
            title = {
                Text("メールカテゴリフィルタ一覧")
            },
        ) {
            when (val loadingState = uiState.loadingState) {
                is SettingMailCategoryFilterScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                    )
                }

                SettingMailCategoryFilterScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize(),
                        onClickRetry = uiState.event::onClickRetry,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: SettingMailCategoryFilterScreenUiState.LoadingState.Loaded,
) {
    Column(modifier = modifier) {
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
            contentPadding = PaddingValues(8.dp),
        ) {
            items(uiState.filters) { item ->
                ItemCard(onClick = { item.event.onClick() }) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.title)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        ProvideTextStyle(
            titleStyle.copy(
                fontFamily = rememberCustomFontFamily(),
            ),
        ) {
            content()
        }
    }
}
