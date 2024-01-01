package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class LoginSettingScreenUiState(
    val event: Event,
    val fidoList: ImmutableList<Fido>,
    val currentSession: Session?,
    val sessionList: ImmutableList<Session>,
    val textInputDialogState: TextInputDialogState?,
) {
    public data class Session(
        val name: String,
        val lastAccess: String,
    )

    public data class TextInputDialogState(
        val title: String,
        val text: String,
        val type: String,
        val onConfirm: (String) -> Unit,
        val onCancel: () -> Unit,
    )

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
    val layoutDirection = LocalLayoutDirection.current
    uiState.textInputDialogState?.also { textInputDialogState ->
        HtmlFullScreenTextInput(
            title = textInputDialogState.title,
            default = textInputDialogState.text,
            inputType = textInputDialogState.type,
            onComplete = { textInputDialogState.onConfirm(it) },
            canceled = { textInputDialogState.onCancel() },
        )
    }
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
            var scrollButtonHeightPx by remember { mutableIntStateOf(0) }
            val lazyListState = rememberLazyListState()
            var listHeightPx by remember { mutableIntStateOf(0) }
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .onSizeChanged {
                        listHeightPx = it.height
                    },
                contentPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                        .plus(24.dp)
                        .plus(with(LocalDensity.current) { scrollButtonHeightPx.toDp() }),
                ),
                state = lazyListState,
            ) {
                item {
                    SettingSmallSection(
                        title = {
                            Text("ログイン方法の追加")
                        },
                    ) {
                        FidoSection(
                            modifier = Modifier.fillMaxWidth()
                                .height(280.dp),
                            fidoList = uiState.fidoList,
                            onClickPlatform = { uiState.event.onClickPlatform() },
                            onClickCrossPlatform = { uiState.event.onClickCrossPlatform() },
                        )
                    }
                }
                item {
                    SettingSmallSection(
                        modifier = Modifier.fillMaxWidth()
                            .height(480.dp),
                        title = {
                            Text("セッション一覧")
                        },
                    ) {
                        if (uiState.currentSession != null) {
                            SessionSection(
                                modifier = Modifier.fillMaxWidth(),
                                currentSession = uiState.currentSession,
                                sessionList = uiState.sessionList,
                            )
                        }
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
                            titleStyle = MaterialTheme.typography.bodyMedium.merge(
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
                    .onSizeChanged { scrollButtonHeightPx = it.height }
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
            val buttonPadding = PaddingValues(
                top = 8.dp,
                bottom = 8.dp,
                start = 18.dp,
                end = 24.dp,
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                contentPadding = buttonPadding,
                onClick = { onClickPlatform() },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "PLATFORM")
            }
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedButton(
                contentPadding = buttonPadding,
                onClick = { onClickCrossPlatform() },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("CROSS_PLATFORM")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
        ) {
            items(fidoList) { fido ->
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f)
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = 12.dp,
                                ),
                            text = fido.name,
                        )
                        IconButton(
                            onClick = { fido.event.onClickDelete() },
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSection(
    modifier: Modifier = Modifier,
    currentSession: LoginSettingScreenUiState.Session,
    sessionList: ImmutableList<LoginSettingScreenUiState.Session>,
) {
    val itemPadding = 4.dp
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 8.dp,
        ),
    ) {
        item {
            Text(
                modifier = Modifier.padding(itemPadding),
                text = "現在のセッション",
                style = MaterialTheme.typography.titleSmall,
            )
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            SessionItem(
                session = currentSession,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                modifier = Modifier.padding(itemPadding),
                text = "その他のセッション",
                style = MaterialTheme.typography.titleSmall,
            )
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        items(sessionList) { session ->
            SessionItem(
                session = session,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = itemPadding),
            )
        }
    }
}

@Composable
private fun SessionItem(
    session: LoginSettingScreenUiState.Session,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(
                top = 8.dp,
            ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideTextStyle(
                MaterialTheme.typography.bodySmall,
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    Text(session.name)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("最終アクセス: ${session.lastAccess}")
                }
            }
            IconButton(
                onClick = { },
            ) {
                Icon(Icons.Default.Menu, contentDescription = "open menu")
            }
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
