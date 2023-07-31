package net.matsudamper.money.frontend.common.ui.screen.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.Popup
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.html.Html

public data class ImportedMailListScreenUiState(
    val event: Event,
    val filters: Filters,
    val loadingState: LoadingState,
    val fullScreenHtml: String?,
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
        val price: String,
        val date: String,
    )

    public data class ImportedMail(
        val mailFrom: String,
        val mailSubject: String,
    )

    @Immutable
    public interface ListItemEvent {
        public fun onClickMailDetail()
        public fun onClickRegisterButton()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
        public fun dismissFullScreenHtml()
    }
}

@Composable
public fun ImportedMailListScreen(
    modifier: Modifier = Modifier,
    uiState: ImportedMailListScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    if (uiState.fullScreenHtml != null) {
        Html(
            html = uiState.fullScreenHtml,
            onDismissRequest = {
                uiState.event.dismissFullScreenHtml()
            },
        )
    }
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Mail,
        listener = rootScreenScaffoldListener,
    ) {
        when (val loadingState = uiState.loadingState) {
            is ImportedMailListScreenUiState.LoadingState.Loaded -> {
                MainContent(
                    modifier = Modifier.fillMaxSize(),
                    uiState = loadingState,
                    filterUiState = uiState.filters,
                )
            }

            is ImportedMailListScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
public fun MainContent(
    modifier: Modifier,
    filterUiState: ImportedMailListScreenUiState.Filters,
    uiState: ImportedMailListScreenUiState.LoadingState.Loaded,
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
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
                .weight(1f),
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
        modifier = modifier
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
                        text = "連携状態:" + when (uiState.link.status) {
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
                    focusable = true,
                    onDismissRequest = {
                        visiblePopup = false
                    },
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.width(IntrinsicSize.Min),
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

@Composable
private fun SuggestUsageItem(
    modifier: Modifier = Modifier,
    listItem: ImportedMailListScreenUiState.ListItem,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp),
        ) {
            val textSpaceHeight = 4.dp

            CardSection(
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
                        textSpaceHeight = textSpaceHeight,
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            CardSection(
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
                                Row {
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
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                            ) {
                                SuggestUsageItem(
                                    modifier = Modifier.weight(1f),
                                    title = suggestUsage.title,
                                    service = suggestUsage.service,
                                    description = suggestUsage.description,
                                    date = suggestUsage.date,
                                    price = suggestUsage.price,
                                    textSpaceHeight = textSpaceHeight,
                                )

                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                ) {
                                    Spacer(Modifier.weight(1f))
                                    OutlinedButton(
                                        onClick = {
                                            listItem.event.onClickRegisterButton()
                                        },
                                    ) {
                                        Text(
                                            text = "登録",
                                            fontFamily = rememberCustomFontFamily(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun CardSection(
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val textHorizontalPadding = 8.dp
    Column {
        Box(
            modifier = Modifier.padding(horizontal = textHorizontalPadding),
        ) {
            title()
        }
        Divider(
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
    textSpaceHeight: Dp,
) {
    val density = LocalDensity.current
    Column(modifier) {
        var titleMaxWidth by remember {
            mutableStateOf(0.dp)
        }
        val minSizeModifier = Modifier
            .widthIn(min = titleMaxWidth)
            .onSizeChanged {
                titleMaxWidth = max(titleMaxWidth, with(density) { it.width.toDp() })
            }
        Column(modifier = Modifier.weight(1f)) {
            MailItemCell(
                title = {
                    Text(
                        modifier = Modifier.then(minSizeModifier),
                        text = "タイトル",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                description = {
                    Text(
                        modifier = Modifier.widthIn(min = titleMaxWidth),
                        text = title,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            Spacer(Modifier.height(textSpaceHeight))
            MailItemCell(
                title = {
                    Text(
                        modifier = Modifier.then(minSizeModifier),
                        text = "サービス",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                description = {
                    Text(
                        modifier = Modifier.widthIn(min = titleMaxWidth),
                        text = service,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            Spacer(Modifier.height(textSpaceHeight))
            MailItemCell(
                title = {
                    Text(
                        modifier = Modifier.then(minSizeModifier),
                        text = "日時",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                description = {
                    Text(
                        text = date,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            Spacer(Modifier.height(textSpaceHeight))
            MailItemCell(
                title = {
                    Text(
                        modifier = Modifier.then(minSizeModifier),
                        text = "説明",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                description = {
                    Text(
                        text = description,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            Spacer(Modifier.height(textSpaceHeight))
            MailItemCell(
                title = {
                    Text(
                        modifier = Modifier.then(minSizeModifier),
                        text = "金額",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                description = {
                    Text(
                        text = price,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
        }
    }
}

@Composable
private fun MailItem(
    modifier: Modifier = Modifier,
    from: String,
    subject: String,
    onClickDetail: () -> Unit,
    textSpaceHeight: Dp,
) {
    val density = LocalDensity.current
    Column(
        modifier = modifier,
    ) {
        var titleMaxWidth by remember {
            mutableStateOf(0.dp)
        }
        val minSizeModifier = Modifier
            .widthIn(min = titleMaxWidth)
            .onSizeChanged {
                titleMaxWidth = max(titleMaxWidth, with(density) { it.width.toDp() })
            }
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(IntrinsicSize.Max),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MailItemCell(
                    title = {
                        Text(
                            modifier = Modifier.then(minSizeModifier),
                            text = "From",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                    description = {
                        Text(
                            text = from,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
                Spacer(Modifier.height(textSpaceHeight))
                MailItemCell(
                    title = {
                        Text(
                            modifier = Modifier.then(minSizeModifier),
                            text = "タイトル",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                    description = {
                        Text(
                            text = subject,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
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
}

@Composable
public fun MailItemCell(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
) {
    Row(modifier = modifier) {
        title()
        Spacer(Modifier.width(12.dp))
        description()
    }
}
