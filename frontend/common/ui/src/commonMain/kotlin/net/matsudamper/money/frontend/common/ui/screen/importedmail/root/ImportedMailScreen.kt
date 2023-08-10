package net.matsudamper.money.frontend.common.ui.screen.importedmail.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.GridColumn
import net.matsudamper.money.frontend.common.ui.lib.applyHtml

public data class MailScreenUiState(
    val loadingState: LoadingState,
    val confirmDialog: AlertDialog?,
    val event: Event,
) {
    public sealed interface LoadingState {

        public object Loading : LoadingState
        public object Error : LoadingState
        public data class Loaded(
            val mail: Mail,
            val usageSuggest: ImmutableList<UsageSuggest>,
            val usage: ImmutableList<LinkedUsage>,
            val hasPlain: Boolean,
            val hasHtml: Boolean,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class AlertDialog(
        val title: String,
        val onClickPositive: () -> Unit,
        val onClickNegative: () -> Unit,
        val onDismissRequest: () -> Unit,
    )

    public data class Mail(
        val from: String,
        val title: String,
        val date: String,
    )

    public data class LinkedUsage(
        val title: String,
        val category: String?,
        val amount: String?,
        val date: String,
    )

    public data class UsageSuggest(
        val title: String,
        val amount: String?,
        val category: String?,
        val description: Clickable,
        val dateTime: String?,
        val event: Event,
    ) {
        public interface Event {
            public fun onClickRegister()
        }
    }

    public data class Clickable(
        val text: String,
        val onClickUrl: (String) -> Unit,
    )

    @Immutable
    public interface LoadedEvent {
        public fun onClickMailHtml()
        public fun onClickMailPlain()
    }

    @Immutable
    public interface Event {
        public fun onClickRetry()
        public fun onClickArrowBackButton()
        public fun onClickTitle()
        public fun onClickDelete()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ImportedMailScreen(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState,
) {
    if (uiState.confirmDialog != null) {
        AlertDialog(
            title = { Text(uiState.confirmDialog.title) },
            onClickPositive = { uiState.confirmDialog.onClickPositive() },
            onClickNegative = { uiState.confirmDialog.onClickNegative() },
            onDismissRequest = { uiState.confirmDialog.onDismissRequest() },
        )
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            KakeBoTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { uiState.event.onClickArrowBackButton() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                onClickTitle = {
                    uiState.event.onClickTitle()
                },
                menu = {
                    var expand by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expand = !expand }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "menu")
                        }
                        DropdownMenu(
                            expanded = expand,
                            onDismissRequest = { expand = false },
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expand = false
                                    uiState.event.onClickDelete()
                                },
                                text = {
                                    Text("削除")
                                },
                            )
                        }
                    }
                },
            ) {
                Text(
                    text = "家計簿 - メール",
                )
            }
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { paddingValues ->
        when (uiState.loadingState) {
            is MailScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            is MailScreenUiState.LoadingState.Loaded -> {
                MainContent(
                    modifier = Modifier.padding(paddingValues),
                    uiState = uiState.loadingState,
                )
            }

            MailScreenUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier.padding(paddingValues),
                    onClickRetry = {
                        uiState.event.onClickRetry()
                    },
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    uiState: MailScreenUiState.LoadingState.Loaded,
) {
    Box(
        modifier = modifier.fillMaxSize()
            .padding(horizontal = 24.dp)
            .widthIn(max = 700.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    text = "メール",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
                Spacer(modifier = Modifier.height(12.dp))
                MailCard(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.mail,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        enabled = uiState.hasPlain,
                        onClick = { uiState.event.onClickMailPlain() },
                    ) {
                        Text("メールテキスト")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        enabled = uiState.hasHtml,
                        onClick = { uiState.event.onClickMailHtml() },
                    ) {
                        Text("メールHTML")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn {
                    if (uiState.usage.isNotEmpty()) {
                        item {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                text = "登録済み",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        items(uiState.usage) { item ->
                            LinkedMoneyUsageCard(
                                modifier = Modifier.fillMaxWidth(),
                                uiState = item,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            text = "解析結果",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(uiState.usageSuggest) { item ->
                        MoneyUsageSuggestCard(
                            modifier = Modifier.fillMaxWidth(),
                            items = item,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { item.event.onClickRegister() },
                            ) {
                                Text("登録")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedMoneyUsageCard(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState.LinkedUsage,
) {
    Card(modifier = modifier) {
        GridColumn(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp),
            horizontalPadding = 8.dp,
            verticalPadding = 4.dp,
        ) {
            row {
                item {
                    Text("タイトル")
                }
                item {
                    Text(text = uiState.title)
                }
            }
            row {
                item {
                    Text("日付")
                }
                item {
                    Text(text = uiState.date)
                }
            }
            row {
                item {
                    Text("カテゴリ")
                }
                item {
                    Text(text = uiState.category.orEmpty())
                }
            }
            row {
                item {
                    Text("金額")
                }
                item {
                    Text(text = uiState.amount.orEmpty())
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun MoneyUsageSuggestCard(
    modifier: Modifier = Modifier,
    items: MailScreenUiState.UsageSuggest,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp),
        ) {
            GridColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalPadding = 8.dp,
                verticalPadding = 4.dp,
            ) {
                row {
                    item {
                        Text("タイトル")
                    }
                    item {
                        Text(text = items.title)
                    }
                }
                row {
                    item {
                        Text("日付")
                    }
                    item {
                        Text(text = items.dateTime.orEmpty())
                    }
                }
                row {
                    item {
                        Text("金額")
                    }
                    item {
                        Text(text = items.amount.orEmpty())
                    }
                }
                row {
                    item {
                        Text("カテゴリ")
                    }
                    item {
                        Text(text = items.category.orEmpty())
                    }
                }
                row {
                    item {
                        Text("説明")
                    }
                    item {
                        val color = MaterialTheme.colorScheme.primary
                        val text = remember(items.description.text) {
                            AnnotatedString(items.description.text)
                                .applyHtml(color)
                        }
                        ClickableText(
                            text = text,
                            style = LocalTextStyle.current.merge(
                                SpanStyle(
                                    color = LocalContentColor.current,
                                ),
                            ),
                            onClick = { index ->
                                text.getUrlAnnotations(index, index).forEach {
                                    items.description.onClickUrl(it.item.url)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MailCard(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState.Mail,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp),
        ) {
            GridColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalPadding = 8.dp,
                verticalPadding = 4.dp,
            ) {
                row {
                    item {
                        Text("From")
                    }
                    item {
                        Text(text = uiState.from)
                    }
                }
                row {
                    item {
                        Text("タイトル")
                    }
                    item {
                        Text(text = uiState.title)
                    }
                }
                row {
                    item {
                        Text("日付")
                    }
                    item {
                        Text(text = uiState.date)
                    }
                }
            }
        }
    }
}
