package net.matsudamper.money.frontend.common.ui.screen.moneyusage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.datetime.LocalDate
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialog
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffold
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.CalendarDialog
import net.matsudamper.money.frontend.common.ui.layout.GridColumn
import net.matsudamper.money.frontend.common.ui.layout.ScrollButton
import net.matsudamper.money.frontend.common.ui.layout.ScrollButtonDefaults
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class MoneyUsageScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val confirmDialog: ConfirmDialog?,
    val textInputDialog: TextInputDialog?,
    val calendarDialog: CalendarDialog?,
    val categorySelectDialog: CategorySelectDialogUiState?,
) {
    public data class CalendarDialog(
        val date: LocalDate,
        val onSelectedDate: (LocalDate) -> Unit,
        val dismissRequest: () -> Unit,
    )

    public data class TextInputDialog(
        val isMultiline: Boolean,
        val title: String,
        val onComplete: (String) -> Unit,
        val onCancel: () -> Unit,
        val default: String,
    )

    public data class ConfirmDialog(
        val title: String,
        val description: String?,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit,
    )

    @Immutable
    public sealed interface LoadingState {

        public object Loading : LoadingState
        public object Error : LoadingState
        public data class Loaded(
            val moneyUsage: MoneyUsage,
            val linkedMails: ImmutableList<MailItem>,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class MoneyUsage(
        val title: String,
        val description: String,
        val amount: String,
        val category: String,
        val dateTime: String,
        val event: MoneyUsageEvent,
    )

    public data class MailItem(
        val subject: String,
        val from: String,
        val date: String,
        val event: MailItemEvent,
    )

    @Immutable
    public interface MoneyUsageEvent {
        public fun onClickTitleChange()
        public fun onClickDateChange()
        public fun onClickCategoryChange()
        public fun onClickDescription()
        public fun onClickAmountChange()
    }

    @Immutable
    public interface MailItemEvent {
        public fun onClick()
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickDelete()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
        public fun onClickRetry()
        public fun onClickBack()
    }
}

@Composable
public fun MoneyUsageScreen(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState,
    kakeboScaffoldListener: KakeboScaffoldListener,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    if (uiState.confirmDialog != null) {
        AlertDialog(
            title = { Text(uiState.confirmDialog.title) },
            onDismissRequest = { uiState.confirmDialog.onDismiss() },
            description = uiState.confirmDialog.description?.let { description -> @Composable { Text(description) } },
            onClickNegative = { uiState.confirmDialog.onDismiss() },
            onClickPositive = { uiState.confirmDialog.onConfirm() },
        )
    }
    if (uiState.textInputDialog != null) {
        HtmlFullScreenTextInput(
            isMultiline = uiState.textInputDialog.isMultiline,
            title = uiState.textInputDialog.title,
            default = uiState.textInputDialog.default,
            onComplete = { uiState.textInputDialog.onComplete(it) },
            canceled = { uiState.textInputDialog.onCancel() },
        )
    }
    if (uiState.calendarDialog != null) {
        CalendarDialog(
            initialCalendar = uiState.calendarDialog.date,
            dismissRequest = {
                uiState.calendarDialog.dismissRequest()
            },
            selectedCalendar = {
                uiState.calendarDialog.onSelectedDate(it)
            },
        )
    }
    if (uiState.categorySelectDialog != null) {
        CategorySelectDialog(
            uiState = uiState.categorySelectDialog,
        )
    }
    KakeboScaffold(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { uiState.event.onClickBack() }) {
                Icon(Icons.Default.ArrowBack, "戻る")
            }
        },
        listener = kakeboScaffoldListener,
    ) { paddingValues ->
        when (val state = uiState.loadingState) {
            is MoneyUsageScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues),
                    uiState = state,
                )
            }

            is MoneyUsageScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            MoneyUsageScreenUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier.fillMaxWidth().padding(paddingValues),
                    onClickRetry = { uiState.event.onClickRetry() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    modifier: Modifier,
    uiState: MoneyUsageScreenUiState.LoadingState.Loaded,
) {
    val density = LocalDensity.current
    val state = rememberLazyListState()
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val height = maxHeight
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(
                end = ScrollButtonDefaults.scrollButtonSize
                    .plus(ScrollButtonDefaults.scrollButtonHorizontalPadding * 2),
            ),
            state = state,
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.padding(12.dp)
                            .weight(1f),
                        text = "使用用途",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Box {
                        var visiblePopup by remember { mutableStateOf(false) }
                        IconButton(onClick = { visiblePopup = !visiblePopup }) {
                            Icon(Icons.Default.MoreVert, "メニュー")
                        }
                        if (visiblePopup) {
                            UsageMenuPopup(
                                onDismissRequest = { visiblePopup = false },
                                onClickDelete = {
                                    visiblePopup = false
                                    uiState.event.onClickDelete()
                                },
                            )
                        }
                    }
                }
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp))

                Card(
                    modifier = Modifier
                        .padding(12.dp),
                ) {
                    MoneyUsage(
                        modifier = Modifier.fillMaxWidth()
                            .padding(12.dp),
                        uiState = uiState.moneyUsage,
                    )
                }
            }
            item {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = "連携されたメール",
                    style = MaterialTheme.typography.titleLarge,
                )
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
            }
            if (uiState.linkedMails.isNotEmpty()) {
                items(uiState.linkedMails) { mail ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(8.dp),
                        onClick = { mail.event.onClick() },
                    ) {
                        MailContent(
                            modifier = Modifier.fillMaxWidth()
                                .padding(12.dp),
                            uiState = mail,
                        )
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("連携されたメールがありません")
                    }
                }
            }
        }
        ScrollButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(ScrollButtonDefaults.scrollButtonHorizontalPadding)
                .width(ScrollButtonDefaults.scrollButtonSize),
            scrollState = state,
            scrollSize = remember(density, height) { with(density) { (height * 0.7f).toPx() } },
        )
    }
}

@Composable
private fun UsageMenuPopup(
    onDismissRequest: () -> Unit,
    onClickDelete: () -> Unit,
) {
    Popup(
        focusable = true,
        onDismissRequest = onDismissRequest,
    ) {
        Card(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp,
            ),
        ) {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
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

@Composable
private fun MailContent(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState.MailItem,
) {
    GridColumn(
        modifier = modifier,
        horizontalPadding = 8.dp,
        verticalPadding = 4.dp,
    ) {
        row {
            item {
                Text(text = "タイトル")
            }
            item {
                Text(text = uiState.subject)
            }
        }
        row {
            item {
                Text(text = "From")
            }
            item {
                Text(text = uiState.from)
            }
        }
        row {
            item {
                Text(text = "日付")
            }
            item {
                Text(text = uiState.date)
            }
        }
    }
}

@Composable
private fun MoneyUsage(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState.MoneyUsage,
) {
    Column(modifier = modifier) {
        MoneyUsageSection(
            title = {
                Text("タイトル")
            },
            content = {
                Text(
                    text = uiState.title,
                )
            },
            onClickChange = {
                uiState.event.onClickTitleChange()
            },
        )
        MoneyUsageSection(
            title = {
                Text("日付")
            },
            content = {
                Text(
                    text = uiState.dateTime,
                )
            },
            onClickChange = {
                uiState.event.onClickDateChange()
            },
        )
        MoneyUsageSection(
            title = {
                Text("金額")
            },
            content = {
                Text(
                    text = uiState.amount,
                )
            },
            onClickChange = {
                uiState.event.onClickAmountChange()
            },
        )
        MoneyUsageSection(
            title = {
                Text("カテゴリ")
            },
            content = {
                Text(
                    text = uiState.category,
                )
            },
            onClickChange = {
                uiState.event.onClickCategoryChange()
            },
        )
        MoneyUsageSection(
            multiline = true,
            title = {
                Text("説明")
            },
            content = {
                Text(
                    text = uiState.description,
                )
            },
            onClickChange = {
                uiState.event.onClickDescription()
            },
        )
    }
}

@Composable
private fun MoneyUsageSection(
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    title: @Composable () -> Unit,
    onClickChange: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium) {
            Box(modifier = Modifier.padding(8.dp)) {
                title()
            }
        }
        Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
        Row(
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                Modifier.weight(1f)
                    .align(
                        if (multiline) {
                            Alignment.Top
                        } else {
                            Alignment.CenterVertically
                        },
                    ),
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                    content()
                }
            }

            OutlinedButton(
                modifier = Modifier.align(
                    if (multiline) {
                        Alignment.Bottom
                    } else {
                        Alignment.CenterVertically
                    },
                ),
                onClick = {
                    onClickChange()
                },
            ) {
                Text("変更")
            }
        }
    }
}
