package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.image.ImageLoadingPlaceholder
import net.matsudamper.money.frontend.common.ui.layout.image.ZoomableImageDialog

@Composable
public fun RootHomeMonthlySubCategoryScreen(
    uiState: RootHomeMonthlySubCategoryScreenUiState,
    showImages: Boolean,
    onToggleShowImages: () -> Unit,
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
                menu = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.padding(end = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            text = "画像",
                        )
                        Switch(
                            checked = showImages,
                            onCheckedChange = { onToggleShowImages() },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
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
                    showImages = showImages,
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
    showImages: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val imageUrlState: MutableState<String?> = remember { mutableStateOf(null) }
        val imageUrl = imageUrlState.value
        if (imageUrl != null) {
            ZoomableImageDialog(
                imageUrl = imageUrl,
                onDismissRequest = { imageUrlState.value = null },
            )
        }
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
            items(loadingState.items) { item ->
                ListItem(
                    item = item,
                    showImages = showImages,
                    onClickImage = { url -> imageUrlState.value = url },
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
    showImages: Boolean,
    onClickImage: (String) -> Unit,
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
            val padding = 12.dp
            Column(
                modifier = Modifier,
            ) {
                Spacer(modifier = Modifier.height(padding))
                Column(
                    modifier = Modifier.padding(horizontal = padding),
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
                if (showImages && item.imageUrls.isNotEmpty()) {
                    val imageRowScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset = available.copy(y = 0f)
                        }
                    }
                    LazyRow(
                        modifier = Modifier.nestedScroll(imageRowScrollConnection),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(padding),
                    ) {
                        items(item.imageUrls) { url ->
                            SubcomposeAsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(80.dp)
                                    .clickable {
                                        onClickImage(url)
                                    },
                                loading = { ImageLoadingPlaceholder() },
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(padding))
                }
            }
        }
    }
}
