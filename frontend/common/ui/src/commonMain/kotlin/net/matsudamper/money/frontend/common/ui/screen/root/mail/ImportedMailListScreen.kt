package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LocalScrollToTopHandler
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.GridColumn
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
public fun ImportedMailListScreen(
    modifier: Modifier = Modifier,
    uiState: ImportedMailListScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }

    RootScreenScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val scrollToTopHandler = LocalScrollToTopHandler.current
        DisposableEffect(scrollToTopHandler, lazyListState) {
            val handler = {
                if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
                    coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                    true
                } else {
                    false
                }
            }
            scrollToTopHandler.register(handler)
            onDispose { scrollToTopHandler.unregister() }
        }
        LaunchedEffect(uiState.operation, coroutineScope) {
            uiState.operation.collect {
                it(
                    object : ImportedMailListScreenUiState.Operation {
                        override fun scrollToTop() {
                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(0)
                            }
                        }
                    },
                )
            }
        }
        var isRefreshing by remember { mutableStateOf(false) }
        when (val loadingState = uiState.loadingState) {
            is ImportedMailListScreenUiState.LoadingState.Loaded -> {
                MainContent(
                    isRefreshing = isRefreshing,
                    modifier = modifier,
                    uiState = loadingState,
                    filterUiState = uiState.filters,
                    moreLoading = uiState.event::moreLoading,
                    lazyListState = lazyListState,
                    refresh = uiState.event::refresh,
                    requestRefresh = { isRefreshing = it },
                )
            }

            is ImportedMailListScreenUiState.LoadingState.Loading -> {
                LaunchedEffect(Unit) {
                    delay(500)
                    isRefreshing = false
                }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    isRefreshing: Boolean,
    filterUiState: ImportedMailListScreenUiState.Filters,
    lazyListState: LazyListState = rememberLazyListState(),
    uiState: ImportedMailListScreenUiState.LoadingState.Loaded,
    moreLoading: () -> Unit,
    requestRefresh: (Boolean) -> Unit,
    refresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Filter(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 12.dp,
                start = 12.dp,
                end = 12.dp,
            ),
            uiState = filterUiState,
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
        ) {
            val refreshState = rememberPullToRefreshState()
            val coroutineScope = rememberCoroutineScope()
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                state = refreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        requestRefresh(true)
                        refresh()
                        delay(1000)
                        requestRefresh(false)
                    }
                },
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                ) {
                    items(uiState.listItems) { mail ->
                        SuggestUsageItem(
                            modifier = Modifier.fillMaxWidth()
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
                                modifier = Modifier.fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LaunchedEffect(Unit) {
                                    delay(500)
                                    requestRefresh(false)
                                }
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Filter(
    modifier: Modifier = Modifier,
    uiState: ImportedMailListScreenUiState.Filters,
    contentPadding: PaddingValues,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth()
                .padding(
                    start = contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                    top = contentPadding.calculateTopPadding(),
                ),
            value = uiState.textSearch.text,
            onValueChange = uiState.textSearch.onTextChanged,
            placeholder = {
                Text(
                    text = "メールを検索",
                    fontFamily = rememberCustomFontFamily(),
                )
            },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = uiState.textSearch.onSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { uiState.textSearch.onSearch() }),
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(
                    bottom = contentPadding.calculateBottomPadding(),
                ),
        ) {
            Spacer(Modifier.width(contentPadding.calculateStartPadding(LayoutDirection.Ltr)))

            Box {
                var visiblePopup by remember { mutableStateOf(false) }
                FilterChip(
                    selected = when (uiState.link.status) {
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
                            text = "連携状態:" +
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
                                            visiblePopup = false
                                            uiState.link.updateState(ImportedMailListScreenUiState.Filters.LinkStatus.Linked)
                                        }
                                        .padding(12.dp),
                                    text = "連携済み",
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth()
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
}

@OptIn(ExperimentalLayoutApi::class)
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
            modifier = Modifier.fillMaxWidth()
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
        HorizontalDivider(
            modifier = Modifier
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
                            modifier = Modifier
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
