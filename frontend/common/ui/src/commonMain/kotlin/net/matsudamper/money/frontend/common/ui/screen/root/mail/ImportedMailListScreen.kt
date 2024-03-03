package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.layout.GridColumn
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

public data class ImportedMailListScreenUiState(
    val event: Event,
    val filters: Filters,
    val loadingState: LoadingState,
) {
    public data class Filters(
        val link: Link,
    ) {
        public data class Link(
            val status: LinkStatus,
            val updateState: (LinkStatus) -> Unit,
        )

        public enum class LinkStatus {
            Undefined,
            Linked,
            NotLinked,
        }
    }

    @Immutable
    public sealed interface LoadingState {
        public data class Loaded(
            val listItems: ImmutableList<ListItem>,
            val showLastLoading: Boolean,
        ) : LoadingState

        public object Loading : LoadingState
    }

    public data class ListItem(
        val mail: ImportedMail,
        val usages: ImmutableList<UsageItem>,
        val event: ListItemEvent,
    )

    public data class UsageItem(
        val title: String,
        val service: String,
        val description: String,
        val amount: String,
        val dateTime: String,
        val category: String,
    )

    public data class ImportedMail(
        val mailFrom: String,
        val mailSubject: String,
    )

    @Immutable
    public interface ListItemEvent {
        public fun onClickMailDetail()

        public fun onClick()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()

        public fun moreLoading()
    }
}

@Composable
public fun ImportedMailListScreen(
    modifier: Modifier = Modifier,
    uiState: ImportedMailListScreenUiState,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    when (val loadingState = uiState.loadingState) {
        is ImportedMailListScreenUiState.LoadingState.Loaded -> {
            MainContent(
                modifier = modifier,
                uiState = loadingState,
                filterUiState = uiState.filters,
                moreLoading = uiState.event::moreLoading,
            )
        }

        is ImportedMailListScreenUiState.LoadingState.Loading -> {
            Box(
                modifier = modifier,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    filterUiState: ImportedMailListScreenUiState.Filters,
    uiState: ImportedMailListScreenUiState.LoadingState.Loaded,
    moreLoading: () -> Unit,
) {
    val density = LocalDensity.current
    var scrollButtonSize by remember { mutableStateOf(0.dp) }
    Column(modifier = modifier) {
        Filter(
            modifier = Modifier.fillMaxWidth(),
            contentPadding =
                PaddingValues(
                    top = 12.dp,
                    start = 12.dp,
                    end = 12.dp,
                ),
            uiState = filterUiState,
        )
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
                    .weight(1f),
        ) {
            val height = maxHeight
            val lazyListState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding =
                    PaddingValues(
                        bottom = scrollButtonSize,
                    ),
            ) {
                items(uiState.listItems) { mail ->
                    SuggestUsageItem(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp,
                                ),
                        listItem = mail,
                    )
                }
                if (uiState.showLastLoading) {
                    item {
                        LaunchedEffect(Unit) {
                            moreLoading()
                            while (isActive) {
                                delay(500)
                                moreLoading()
                            }
                        }
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            ScrollButtons(
                modifier =
                    Modifier
                        .onSizeChanged {
                            scrollButtonSize = with(density) { it.height.toDp() }
                        }
                        .align(Alignment.BottomEnd)
                        .padding(ScrollButtonsDefaults.padding)
                        .height(ScrollButtonsDefaults.height),
                scrollState = lazyListState,
                scrollSize = with(density) { height.toPx() } * 0.4f,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Filter(
    modifier: Modifier = Modifier,
    uiState: ImportedMailListScreenUiState.Filters,
    contentPadding: PaddingValues,
) {
    Row(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
    ) {
        Spacer(Modifier.width(contentPadding.calculateStartPadding(LayoutDirection.Ltr)))

        Box {
            var visiblePopup by remember { mutableStateOf(false) }
            FilterChip(
                selected =
                    when (uiState.link.status) {
                        ImportedMailListScreenUiState.Filters.LinkStatus.Undefined -> false
                        ImportedMailListScreenUiState.Filters.LinkStatus.Linked,
                        ImportedMailListScreenUiState.Filters.LinkStatus.NotLinked,
                        -> true
                    },
                onClick = {
                    when (uiState.link.status) {
                        ImportedMailListScreenUiState.Filters.LinkStatus.Undefined -> {
                            visiblePopup = true
                        }

                        ImportedMailListScreenUiState.Filters.LinkStatus.Linked,
                        ImportedMailListScreenUiState.Filters.LinkStatus.NotLinked,
                        -> {
                            uiState.link.updateState(ImportedMailListScreenUiState.Filters.LinkStatus.Undefined)
                        }
                    }
                },
                label = {
                    Text(
                        text =
                            "連携状態:" +
                                when (uiState.link.status) {
                                    ImportedMailListScreenUiState.Filters.LinkStatus.Undefined -> "全て"
                                    ImportedMailListScreenUiState.Filters.LinkStatus.Linked -> "連携済み"
                                    ImportedMailListScreenUiState.Filters.LinkStatus.NotLinked -> "未連携"
                                },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
            if (visiblePopup) {
                Popup(
                    alignment = Alignment.BottomStart,
                    onDismissRequest = {
                        visiblePopup = false
                    },
                    properties = PopupProperties(focusable = true),
                    onPreviewKeyEvent = { false },
                    onKeyEvent = { false },
                ) {
                    Card(
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 8.dp,
                            ),
                    ) {
                        Column(
                            modifier = Modifier.width(IntrinsicSize.Max),
                        ) {
                            Text(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            visiblePopup = false
                                            uiState.link.updateState(ImportedMailListScreenUiState.Filters.LinkStatus.Linked)
                                        }
                                        .padding(12.dp),
                                text = "連携済み",
                            )
                            Text(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            visiblePopup = false
                                            uiState.link.updateState(ImportedMailListScreenUiState.Filters.LinkStatus.NotLinked)
                                        }
                                        .padding(12.dp),
                                text = "未連携",
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(contentPadding.calculateEndPadding(LayoutDirection.Ltr)))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SuggestUsageItem(
    modifier: Modifier = Modifier,
    listItem: ImportedMailListScreenUiState.ListItem,
) {
    Card(
        modifier = modifier,
        onClick = { listItem.event.onClick() },
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(12.dp),
        ) {
            CardSection(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = "メール",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                content = {
                    MailItem(
                        modifier = Modifier.fillMaxWidth(),
                        from = listItem.mail.mailFrom,
                        subject = listItem.mail.mailSubject,
                        onClickDetail = {
                            listItem.event.onClickMailDetail()
                        },
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            CardSection(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = "解析結果",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                content = {
                    when (listItem.usages.size) {
                        0 -> {
                            Text(
                                text = "解析できませんでした。",
                                fontFamily = rememberCustomFontFamily(),
                            )
                        }

                        else -> {
                            var page by remember { mutableStateOf(0) }
                            if (listItem.usages.size > 1) {
                                FlowRow {
                                    repeat(listItem.usages.size) { index ->
                                        OutlinedButton(
                                            onClick = {
                                                page = index
                                            },
                                        ) {
                                            Text(
                                                text = (index + 1).toString(),
                                                fontFamily = rememberCustomFontFamily(),
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            val suggestUsage by remember(listItem.usages, page) {
                                mutableStateOf(listItem.usages[page])
                            }
                            SuggestUsageItem(
                                modifier = Modifier.fillMaxWidth(),
                                title = suggestUsage.title,
                                service = suggestUsage.service,
                                description = suggestUsage.description,
                                date = suggestUsage.dateTime,
                                price = suggestUsage.amount,
                                category = suggestUsage.category,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun CardSection(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val textHorizontalPadding = 8.dp
    Column(modifier = modifier) {
        Box(
            modifier = Modifier.padding(horizontal = textHorizontalPadding),
        ) {
            title()
        }
        Divider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(1.dp),
            color = Color.White,
        )
        Column(
            modifier = Modifier.padding(horizontal = textHorizontalPadding),
        ) {
            content()
        }
    }
}

@Composable
private fun SuggestUsageItem(
    modifier: Modifier = Modifier,
    title: String,
    service: String,
    description: String,
    date: String,
    price: String,
    category: String,
) {
    GridColumn(
        modifier = modifier,
        horizontalPadding = 8.dp,
        verticalPadding = 4.dp,
    ) {
        row {
            item {
                Text(
                    text = "タイトル",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
            item {
                Text(
                    text = title,
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
        row {
            item {
                Text(
                    text = "サービス名",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
            item {
                Text(
                    text = service,
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
        row {
            item {
                Text(
                    text = "日時",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
            item {
                Text(
                    text = date,
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
        row {
            item {
                Text(
                    text = "カテゴリ",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
            item {
                Text(
                    text = category,
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
        row {
            item {
                Text(
                    text = "金額",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
            item {
                Text(
                    text = price,
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
        row {
            item {
                Text(
                    text = "説明",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
            item {
                Text(
                    text = description,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
    }
}

@Composable
private fun MailItem(
    modifier: Modifier = Modifier,
    from: String,
    subject: String,
    onClickDetail: () -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        GridColumn(
            modifier = Modifier.weight(1f),
            horizontalPadding = 8.dp,
            verticalPadding = 4.dp,
        ) {
            row {
                item {
                    Text(
                        modifier = Modifier,
                        text = "From",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
                item {
                    Text(
                        modifier = Modifier,
                        text = from,
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
            row {
                item {
                    Column(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            modifier =
                                Modifier
                                    .fillMaxHeight(),
                            text = "タイトル",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                }
                item {
                    Text(
                        modifier = Modifier,
                        text = subject,
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxHeight(),
        ) {
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { onClickDetail() },
            ) {
                Text(
                    text = "詳細",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
    }
}
