package net.matsudamper.money.frontend.common.ui.screen.root.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.layout.GridColumn

public data class NotificationUsageDetailScreenUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object NotFound : LoadingState

        public data class Loaded(
            val notification: Notification,
            val filter: Filter,
            val draft: Draft?,
            val canRegister: Boolean,
            val linkedUsage: LinkedUsageState,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class Notification(
        val packageName: String,
        val status: String,
        val postedAt: String,
        val receivedAt: String,
        val text: String,
    )

    public sealed interface Filter {
        public data object NotMatched : Filter

        public data class Matched(
            val title: String,
            val description: String,
        ) : Filter
    }

    public data class Draft(
        val title: String,
        val description: String,
        val amount: String,
        val dateTime: String,
        val subCategory: String,
    )

    public sealed interface LinkedUsageState {
        public data object None : LinkedUsageState

        public data object Loading : LinkedUsageState

        public data object MissingUsageId : LinkedUsageState

        public data object Error : LinkedUsageState

        public data class Loaded(
            val usage: LinkedUsage,
        ) : LinkedUsageState
    }

    public data class LinkedUsage(
        val title: String,
        val category: String,
        val amount: String,
        val dateTime: String,
        val event: LinkedUsageEvent,
    )

    @Immutable
    public interface LinkedUsageEvent {
        public fun onClick()
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickRegister()
    }

    @Immutable
    public interface Event {
        public fun onClickBack()

        public fun onClickTitle()
    }
}

@Composable
public fun NotificationUsageDetailScreen(
    uiState: NotificationUsageDetailScreenUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.event.onClickTitle()
                        },
                        text = "家計簿 - 通知",
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) { paddingValues ->
        when (val loadingState = uiState.loadingState) {
            NotificationUsageDetailScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            NotificationUsageDetailScreenUiState.LoadingState.NotFound -> {
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("通知が見つかりません。")
                }
            }

            is NotificationUsageDetailScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.padding(paddingValues),
                    uiState = loadingState,
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier,
    uiState: NotificationUsageDetailScreenUiState.LoadingState.Loaded,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 700.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
        ) {
            item {
                SectionTitle("通知")
                NotificationCard(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.notification,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                SectionTitle("フィルター")
                FilterCard(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.filter,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (uiState.draft != null) {
                item {
                    SectionTitle("解析結果")
                    DraftCard(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState.draft,
                    )
                    if (uiState.canRegister) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { uiState.event.onClickRegister() },
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("登録")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            item {
                SectionTitle("登録済み")
                LinkedUsageContent(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.linkedUsage,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 12.dp),
        text = text,
        style = MaterialTheme.typography.headlineLarge,
    )
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun NotificationCard(
    modifier: Modifier = Modifier,
    uiState: NotificationUsageDetailScreenUiState.Notification,
) {
    Card(modifier = modifier) {
        GridColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalPadding = 8.dp,
            verticalPadding = 4.dp,
        ) {
            row {
                item { Text("パッケージ") }
                item { Text(uiState.packageName) }
            }
            row {
                item { Text("状態") }
                item { Text(uiState.status) }
            }
            row {
                item { Text("通知日時") }
                item { Text(uiState.postedAt) }
            }
            row {
                item { Text("取得日時") }
                item { Text(uiState.receivedAt) }
            }
            row {
                item { Text("本文") }
                item { Text(uiState.text) }
            }
        }
    }
}

@Composable
private fun FilterCard(
    modifier: Modifier = Modifier,
    uiState: NotificationUsageDetailScreenUiState.Filter,
) {
    Card(modifier = modifier) {
        when (uiState) {
            NotificationUsageDetailScreenUiState.Filter.NotMatched -> {
                Text(
                    modifier = Modifier.padding(20.dp),
                    text = "一致するフィルターはありません。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is NotificationUsageDetailScreenUiState.Filter.Matched -> {
                GridColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalPadding = 8.dp,
                    verticalPadding = 4.dp,
                ) {
                    row {
                        item { Text("状態") }
                        item { Text("一致") }
                    }
                    row {
                        item { Text("フィルター") }
                        item { Text(uiState.title) }
                    }
                    row {
                        item { Text("説明") }
                        item { Text(uiState.description) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftCard(
    modifier: Modifier = Modifier,
    uiState: NotificationUsageDetailScreenUiState.Draft,
) {
    Card(modifier = modifier) {
        GridColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalPadding = 8.dp,
            verticalPadding = 4.dp,
        ) {
            row {
                item { Text("タイトル") }
                item { Text(uiState.title) }
            }
            row {
                item { Text("日付") }
                item { Text(uiState.dateTime) }
            }
            row {
                item { Text("カテゴリ") }
                item { Text(uiState.subCategory) }
            }
            row {
                item { Text("金額") }
                item { Text(uiState.amount) }
            }
            row {
                item { Text("説明") }
                item { Text(uiState.description) }
            }
        }
    }
}

@Composable
private fun LinkedUsageContent(
    modifier: Modifier = Modifier,
    uiState: NotificationUsageDetailScreenUiState.LinkedUsageState,
) {
    when (uiState) {
        NotificationUsageDetailScreenUiState.LinkedUsageState.None -> {
            EmptyText(modifier = modifier, text = "まだ使用用途に追加されていません。")
        }

        NotificationUsageDetailScreenUiState.LinkedUsageState.Loading -> {
            Box(
                modifier = modifier.height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        NotificationUsageDetailScreenUiState.LinkedUsageState.MissingUsageId -> {
            EmptyText(modifier = modifier, text = "追加済みですが、使用用途IDが保存されていません。")
        }

        NotificationUsageDetailScreenUiState.LinkedUsageState.Error -> {
            EmptyText(modifier = modifier, text = "使用用途を取得できませんでした。")
        }

        is NotificationUsageDetailScreenUiState.LinkedUsageState.Loaded -> {
            LinkedUsageCard(
                modifier = modifier,
                uiState = uiState.usage,
            )
        }
    }
}

@Composable
private fun LinkedUsageCard(
    modifier: Modifier = Modifier,
    uiState: NotificationUsageDetailScreenUiState.LinkedUsage,
) {
    Card(
        modifier = modifier.clickable { uiState.event.onClick() },
    ) {
        GridColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalPadding = 8.dp,
            verticalPadding = 4.dp,
        ) {
            row {
                item { Text("タイトル") }
                item { Text(uiState.title) }
            }
            row {
                item { Text("日付") }
                item { Text(uiState.dateTime) }
            }
            row {
                item { Text("カテゴリ") }
                item { Text(uiState.category) }
            }
            row {
                item { Text("金額") }
                item { Text(uiState.amount) }
            }
        }
    }
}

@Composable
private fun EmptyText(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier.padding(20.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium,
    )
}
