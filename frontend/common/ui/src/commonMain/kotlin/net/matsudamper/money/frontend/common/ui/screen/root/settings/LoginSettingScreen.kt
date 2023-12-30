package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class LoginSettingScreenUiState(
    val event: Event,
    val fidoList: ImmutableList<Fido>,
) {
    public data class Fido(
        val name: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClickDelete()
        }
    }

    @Immutable
    public interface Event {
        public fun onClickBack()
        public fun onClickPlatform()
        public fun onClickCrossPlatform()
        public fun onClickLogout()
    }
}

@Composable
public fun LoginSettingScreen(
    uiState: LoginSettingScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    modifier: Modifier = Modifier,
) {
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Settings,
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
        listener = rootScreenScaffoldListener,
    ) {
        SettingScaffold(
            title = {
                Text("ログイン設定")
            },
        ) { paddingValues ->
            val lazyListState = rememberLazyListState()
            var listHeightPx by remember { mutableIntStateOf(0) }
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .onSizeChanged {
                        listHeightPx = it.height
                    },
                contentPadding = paddingValues,
                state = lazyListState,
            ) {
                item {
                    SettingSmallSection(
                        title = {
                            Text("ログイン方法の追加")
                        },
                    ) {
                        FidoSection(
                            modifier = Modifier.fillMaxWidth(),
                            fidoList = uiState.fidoList,
                            onClickPlatform = { uiState.event.onClickPlatform() },
                            onClickCrossPlatform = { uiState.event.onClickCrossPlatform() },
                        )
                    }
                }
                item {
                    SettingSmallSection(
                        title = {
                            Text("セッション一覧")
                        },
                    ) {
                        SessionSection(
                            modifier = Modifier.fillMaxWidth()
                                .height(280.dp),
                        )
                    }
                }
                item {
                    SettingSmallSection(
                        title = {
                            Text("その他")
                        },
                    ) {
                        SettingListMenuItemButton(
                            modifier = Modifier.fillMaxWidth(),
                            titleStyle = MaterialTheme.typography.titleLarge.merge(
                                TextStyle(
                                    color = MaterialTheme.colorScheme.error,
                                ),
                            ),
                            onClick = { uiState.event.onClickLogout() },
                        ) {
                            Text("ログアウト")
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
                scrollSize = listHeightPx * 0.4f,
            )
        }
    }
}

@Composable
private fun FidoSection(
    fidoList: ImmutableList<LoginSettingScreenUiState.Fido>,
    onClickPlatform: () -> Unit,
    onClickCrossPlatform: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { onClickPlatform() },
            ) {
                Text("PLATFORM")
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(
                onClick = { onClickCrossPlatform() },
            ) {
                Text("CROSS_PLATFORM")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
        ) {
            items(fidoList) { fido ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = fido.name,
                    )
                    IconButton(
                        onClick = { fido.event.onClickDelete() },
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSection(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items((0 until 5).toList()) {
            Text("TODO: $it")
        }
    }
}

@Composable
private fun SettingSmallSection(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        ProvideTextStyle(
            MaterialTheme.typography.titleMedium,
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        top = 16.dp,
                        bottom = 8.dp,
                    )
                    .padding(
                        horizontal = 8.dp,
                    ),
            ) {
                title()
            }
        }
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
