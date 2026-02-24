package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold

@Composable
public fun RootHomeMonthlySubCategoryScreen(
    uiState: RootHomeMonthlySubCategoryScreenUiState,
    modifier: Modifier = Modifier,
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
                    Text(uiState.headerTitle)
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        when (val loadingState = uiState.loadingState) {
            is RootHomeMonthlySubCategoryScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.fillMaxWidth(),
                    loadingState = loadingState,
                    uiState = uiState,
                )
            }

            RootHomeMonthlySubCategoryScreenUiState.LoadingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            RootHomeMonthlySubCategoryScreenUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier,
                    onClickRetry = { /* TODO */ },
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    loadingState: RootHomeMonthlySubCategoryScreenUiState.LoadingState.Loaded,
    uiState: RootHomeMonthlySubCategoryScreenUiState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val lazyListState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                bottom = 8.dp,
                top = 8.dp,
                start = 8.dp,
                end = 8.dp,
            ),
        ) {
            item {
                Text(
                    text = "${loadingState.categoryName} > ${loadingState.subCategoryName}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            item {
                SortSection(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    onSortTypeChanged = { type ->
                        uiState.event.onSortTypeChanged(type)
                    },
                    currentSortType = uiState.currentSortType,
                    onSortOrderChanged = { order ->
                        uiState.event.onSortOrderChanged(order)
                    },
                    sortOrderType = uiState.sortOrder,
                )
            }
            items(loadingState.items, key = { it.id }) { item ->
                ListItem(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                )
            }
            if (loadingState.hasMoreItem) {
                item {
                    LaunchedEffect(Unit) {
                        loadingState.event.loadMore()
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 16.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
private fun ListItem(
    item: RootHomeMonthlySubCategoryScreenUiState.Item,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        onClick = {
            item.event.onClick()
        },
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.bodyMedium,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = item.date,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.title,
                        maxLines = 3,
                    )
                    Text(
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .height(IntrinsicSize.Max)
                            .requiredWidthIn(min = 80.dp),
                        text = item.category,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .height(IntrinsicSize.Max)
                            .requiredWidthIn(min = 60.dp),
                        maxLines = 1,
                        text = item.amount,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
