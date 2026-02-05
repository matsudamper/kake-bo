package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.base.SharedNavigation
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput

public data class LoginSettingScreenUiState(
    val textInputDialogState: TextInputDialogState?,
    val loadingState: LoadingState,
    val event: Event,
    val rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    @Immutable
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val fidoList: ImmutableList<Fido>,
            val currentSession: Session,
            val sessionList: ImmutableList<Session>,
        ) : LoadingState
    }

    public data class Session(
        val name: String,
        val lastAccess: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClickDelete()

            public fun onClickNameChange()
        }
    }

    public data class TextInputDialogState(
        val title: String,
        val text: String,
        val type: TextFieldType,
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
    navigationUi: SharedNavigation,
    modifier: Modifier = Modifier,
    windowInsets: PaddingValues,
) {
    uiState.textInputDialogState?.also { textInputDialogState ->
        FullScreenTextInput(
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
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.rootScreenScaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
        listener = uiState.rootScreenScaffoldListener,
        navigationUi = navigationUi,
    ) {
        SettingScaffold(
            title = {
                Text("ログイン設定")
            },
        ) { paddingValues ->
            when (uiState.loadingState) {
                is LoginSettingScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        uiState = uiState.loadingState,
                        event = uiState.event,
                        paddingValues = paddingValues,
                    )
                }

                is LoginSettingScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    uiState: LoginSettingScreenUiState.LoadingState.Loaded,
    event: LoginSettingScreenUiState.Event,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val layoutDirection = LocalLayoutDirection.current
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
                    .plus(24.dp),
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
                        onClickPlatform = { event.onClickPlatform() },
                        onClickCrossPlatform = { event.onClickCrossPlatform() },
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
                    SessionSection(
                        modifier = Modifier.fillMaxWidth(),
                        currentSession = uiState.currentSession,
                        sessionList = uiState.sessionList,
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
                        titleStyle = MaterialTheme.typography.bodyMedium.merge(
                            TextStyle(
                                color = MaterialTheme.colorScheme.error,
                            ),
                        ),
                        onClick = { event.onClickLogout() },
                    ) {
                        Text("ログアウト")
                    }
                }
            }
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
    val itemHorizontalPadding = 4.dp
    val titleVerticalPadding = 8.dp
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 8.dp,
        ),
    ) {
        item {
            Text(
                modifier = Modifier.padding(
                    horizontal = itemHorizontalPadding,
                    vertical = titleVerticalPadding,
                ),
                text = "現在のセッション",
                style = MaterialTheme.typography.titleSmall,
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        item {
            SessionItem(
                session = currentSession,
                modifier = Modifier.fillMaxWidth(),
                onClickDelete = null,
                onClickNameChange = { currentSession.event.onClickNameChange() },
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Text(
                modifier = Modifier.padding(
                    horizontal = itemHorizontalPadding,
                    vertical = titleVerticalPadding,
                ),
                text = "その他のセッション",
                style = MaterialTheme.typography.titleSmall,
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        items(sessionList) { session ->
            SessionItem(
                session = session,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = itemHorizontalPadding),
                onClickDelete = { session.event.onClickDelete() },
                onClickNameChange = null,
            )
        }
    }
}

@Composable
private fun SessionItem(
    session: LoginSettingScreenUiState.Session,
    onClickDelete: (() -> Unit)?,
    onClickNameChange: (() -> Unit)?,
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
            var visibleMenu by remember { mutableStateOf(false) }
            IconButton(
                onClick = { visibleMenu = !visibleMenu },
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "open menu")
                if (visibleMenu) {
                    Popup(
                        onDismissRequest = { visibleMenu = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        Card(
                            modifier = Modifier.width(IntrinsicSize.Max),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 8.dp,
                            ),
                        ) {
                            if (onClickNameChange != null) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable {
                                                visibleMenu = false
                                                onClickNameChange()
                                            }
                                            .padding(12.dp),
                                        text = "名前の変更",
                                    )
                                }
                            }
                            if (onClickDelete != null) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable {
                                                visibleMenu = false
                                                onClickDelete()
                                            }
                                            .padding(12.dp),
                                        color = MaterialTheme.colorScheme.error,
                                        text = "削除",
                                    )
                                }
                            }
                        }
                    }
                }
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
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
