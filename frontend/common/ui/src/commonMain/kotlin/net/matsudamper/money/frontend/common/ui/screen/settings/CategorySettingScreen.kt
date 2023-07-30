package net.matsudamper.money.frontend.common.ui.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
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
        public object Loading : LoadingState
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

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Settings,
        listener = rootScreenScaffoldListener,
        content = {
            MainContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                        end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                    ),
                ) {
                    item {
                        Spacer(Modifier.height(24.dp))
                    }
                    stickyHeader {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = uiState.categoryName,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            FlowRow {
                                Spacer(modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { uiState.event.onClickChangeCategoryName() },
                                    modifier = Modifier,
                                ) {
                                    Text(text = "カテゴリー名変更")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = { uiState.event.onClickAddSubCategoryButton() },
                                    modifier = Modifier,
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text(text = "サブカテゴリーを追加")
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                    }
                    items(
                        items = state.item,
                    ) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 4.dp,
                                ),
                            onClick = { item.event.onClick() },
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(
                                        horizontal = 24.dp,
                                        vertical = 24.dp,
                                    ),
                                text = item.name,
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            is SettingCategoryScreenUiState.LoadingState.Loading -> {
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
