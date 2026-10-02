package net.matsudamper.money.frontend.common.ui.screen.importedmail.root

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_add
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_more_vert
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.GridColumn
import net.matsudamper.money.frontend.common.ui.layout.UrlClickableText
import net.matsudamper.money.frontend.common.ui.layout.UrlMenuDialog
import org.jetbrains.compose.resources.painterResource

public data class MailScreenUiState(
    val loadingState: LoadingState,
    val confirmDialog: AlertDialog?,
    val urlMenuDialog: UrlMenuDialog?,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object Error : LoadingState

        public data class Loaded(
            val mail: Mail,
            val usageSuggest: ImmutableList<UsageSuggest>,
            val aiParse: AiParse,
            val usage: ImmutableList<LinkedUsage>,
            val hasPlain: Boolean,
            val hasHtml: Boolean,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class AiParse(
        val state: AiParseState,
        val startErrorMessage: String?,
    )

    public sealed interface AiParseState {
        public data object NotExecuted : AiParseState

        public data object Running : AiParseState

        public data class Failed(
            val message: String,
        ) : AiParseState

        public data class Succeeded(
            val usageSuggest: ImmutableList<UsageSuggest>,
        ) : AiParseState
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
        val event: LinkedUsageEvent,
    )

    @Immutable
    public interface LinkedUsageEvent {
        public fun onClick()
    }

    public data class UsageSuggest(
        val title: String,
        val amount: String?,
        val category: String?,
        val description: Clickable,
        val dateTime: String?,
        val event: Event,
        val serviceName: String,
    ) {
        public interface Event {
            public fun onClickRegister()
        }
    }

    public data class Clickable(
        val text: String,
        val event: ClickableEvent,
    )

    @Immutable
    public interface ClickableEvent {
        public fun onClickUrl(url: String)

        public fun onLongClickUrl(text: String)
    }

    public data class UrlMenuDialog(
        val url: String,
        val event: UrlMenuDialogEvent,
    )

    @Immutable
    public interface UrlMenuDialogEvent {
        public fun onClickOpen()

        public fun onClickCopy()

        public fun onDismissRequest()
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickMailHtml()

        public fun onClickMailPlain()

        public fun onClickRegister()

        public fun onClickAiParse()
    }

    @Immutable
    public interface Event {
        public fun onClickRetry()

        public fun onClickArrowBackButton()

        public fun onClickTitle()

        public fun onClickDelete()
        public fun onResume()
    }
}

@Composable
public fun ImportedMailScreen(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState,
    windowInsets: PaddingValues,
) {
    if (uiState.confirmDialog != null) {
        AlertDialog(
            title = { Text(uiState.confirmDialog.title) },
            onClickPositive = { uiState.confirmDialog.onClickPositive() },
            onClickNegative = { uiState.confirmDialog.onClickNegative() },
            onDismissRequest = { uiState.confirmDialog.onDismissRequest() },
        )
    }
    if (uiState.urlMenuDialog != null) {
        UrlMenuDialog(
            url = uiState.urlMenuDialog.url,
            onClickOpen = { uiState.urlMenuDialog.event.onClickOpen() },
            onClickCopy = { uiState.urlMenuDialog.event.onClickCopy() },
            onDismissRequest = { uiState.urlMenuDialog.event.onDismissRequest() },
        )
    }
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickArrowBackButton() }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                menu = {
                    var expand by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expand = !expand }) {
                            Icon(painter = painterResource(Res.drawable.ic_more_vert), contentDescription = "menu")
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
                title = {
                    Box(
                        modifier = Modifier
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                uiState.event.onClickTitle()
                            },
                    ) {
                        Text(
                            text = "家計簿 - メール",
                        )
                    }
                },
                windowInsets = windowInsets,
            )
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
    val lazyListState = rememberLazyListState()
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(),
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 700.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                MailSection(
                    uiState = uiState,
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (uiState.usage.isNotEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        text = "登録済み",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
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
                HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (uiState.usageSuggest.isEmpty()) {
                item {
                    UsageSuggestEmptyContent(
                        modifier = Modifier.fillMaxWidth(),
                        onClickRegister = { uiState.event.onClickRegister() },
                    )
                }
            } else {
                items(uiState.usageSuggest) { item ->
                    MoneyUsageSuggestItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = item,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                AiParseHeader(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.aiParse,
                    onClickAiParse = { uiState.event.onClickAiParse() },
                )
            }
            when (val aiParseState = uiState.aiParse.state) {
                is MailScreenUiState.AiParseState.Succeeded -> {
                    if (aiParseState.usageSuggest.isEmpty()) {
                        item {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = "支払いは見つかりませんでした",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        items(aiParseState.usageSuggest) { item ->
                            MoneyUsageSuggestItem(
                                modifier = Modifier.fillMaxWidth(),
                                item = item,
                            )
                        }
                    }
                }

                is MailScreenUiState.AiParseState.Failed,
                MailScreenUiState.AiParseState.NotExecuted,
                MailScreenUiState.AiParseState.Running,
                -> Unit
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MoneyUsageSuggestItem(
    modifier: Modifier = Modifier,
    item: MailScreenUiState.UsageSuggest,
) {
    Column(modifier = modifier) {
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

@Composable
private fun AiParseHeader(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState.AiParse,
    onClickAiParse: () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp)
                    .weight(1f),
                text = "AI解析結果",
                style = MaterialTheme.typography.headlineLarge,
            )
            val isRunning = uiState.state is MailScreenUiState.AiParseState.Running
            OutlinedButton(
                enabled = isRunning.not(),
                onClick = { onClickAiParse() },
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("解析中")
                } else {
                    Text(
                        text = when (uiState.state) {
                            MailScreenUiState.AiParseState.NotExecuted -> "AIで解析"
                            else -> "再解析"
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
        Spacer(modifier = Modifier.height(12.dp))
        val message = uiState.startErrorMessage ?: when (val state = uiState.state) {
            is MailScreenUiState.AiParseState.Failed -> "解析に失敗しました: ${state.message}"
            MailScreenUiState.AiParseState.NotExecuted -> "Gemini APIでメールを解析します"
            MailScreenUiState.AiParseState.Running,
            is MailScreenUiState.AiParseState.Succeeded,
            -> null
        }
        if (message != null) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MailSection(uiState: MailScreenUiState.LoadingState.Loaded) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = "メール",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
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
}

@Composable
private fun UsageSuggestEmptyContent(
    modifier: Modifier = Modifier,
    onClickRegister: () -> Unit,
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "解析できませんでした",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onClickRegister() },
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_add),
                contentDescription = null,
            )
            Text("手動登録")
        }
    }
}

@Composable
private fun LinkedMoneyUsageCard(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState.LinkedUsage,
) {
    Card(
        modifier = modifier
            .clickable {
                uiState.event.onClick()
            },
    ) {
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
                        Text("サービス名")
                    }
                    item {
                        Text(text = items.serviceName)
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
                        Text("カテゴリ")
                    }
                    item {
                        Text(text = items.category.orEmpty())
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
                        Text("説明")
                    }
                    item {
                        UrlClickableText(
                            text = items.description.text,
                            onClickUrl = { items.description.event.onClickUrl(it) },
                            onLongClickUrl = { items.description.event.onLongClickUrl(it) },
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

@Composable
@Preview
private fun ImportedMailScreenAiParseNotExecutedPreview() {
    ImportedMailScreenPreviewContent(
        aiParse = MailScreenUiState.AiParse(
            state = MailScreenUiState.AiParseState.NotExecuted,
            startErrorMessage = null,
        ),
    )
}

@Composable
@Preview
private fun ImportedMailScreenAiParseRunningPreview() {
    ImportedMailScreenPreviewContent(
        aiParse = MailScreenUiState.AiParse(
            state = MailScreenUiState.AiParseState.Running,
            startErrorMessage = null,
        ),
    )
}

@Composable
@Preview
private fun ImportedMailScreenAiParseFailedPreview() {
    ImportedMailScreenPreviewContent(
        aiParse = MailScreenUiState.AiParse(
            state = MailScreenUiState.AiParseState.Failed(
                message = "Gemini APIがエラーを返しました(400): API key not valid",
            ),
            startErrorMessage = null,
        ),
    )
}

@Composable
@Preview
private fun ImportedMailScreenAiParseSucceededPreview() {
    ImportedMailScreenPreviewContent(
        aiParse = MailScreenUiState.AiParse(
            state = MailScreenUiState.AiParseState.Succeeded(
                usageSuggest = immutableListOf(
                    createPreviewUsageSuggest(
                        title = "サンプルストア",
                        serviceName = "",
                    ),
                ),
            ),
            startErrorMessage = null,
        ),
    )
}

@Composable
private fun ImportedMailScreenPreviewContent(aiParse: MailScreenUiState.AiParse) {
    AppRoot {
        ImportedMailScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = MailScreenUiState(
                loadingState = MailScreenUiState.LoadingState.Loaded(
                    mail = MailScreenUiState.Mail(
                        from = "noreply@example.com",
                        title = "ご注文ありがとうございます",
                        date = "2026/01/01 12:00",
                    ),
                    usageSuggest = immutableListOf(),
                    aiParse = aiParse,
                    usage = immutableListOf(),
                    hasPlain = true,
                    hasHtml = true,
                    event = object : MailScreenUiState.LoadedEvent {
                        override fun onClickMailHtml() {}
                        override fun onClickMailPlain() {}
                        override fun onClickRegister() {}
                        override fun onClickAiParse() {}
                    },
                ),
                confirmDialog = null,
                urlMenuDialog = null,
                event = object : MailScreenUiState.Event {
                    override fun onClickRetry() {}
                    override fun onClickArrowBackButton() {}
                    override fun onClickTitle() {}
                    override fun onClickDelete() {}
                    override fun onResume() {}
                },
            ),
            windowInsets = PaddingValues(0.dp),
        )
    }
}

private fun createPreviewUsageSuggest(
    title: String,
    serviceName: String,
): MailScreenUiState.UsageSuggest {
    return MailScreenUiState.UsageSuggest(
        title = title,
        amount = "1,000円",
        category = "食費 / 外食",
        description = MailScreenUiState.Clickable(
            text = "商品A x1",
            event = object : MailScreenUiState.ClickableEvent {
                override fun onClickUrl(url: String) {}
                override fun onLongClickUrl(text: String) {}
            },
        ),
        dateTime = "2026/01/01 12:00",
        event = object : MailScreenUiState.UsageSuggest.Event {
            override fun onClickRegister() {}
        },
        serviceName = serviceName,
    )
}
