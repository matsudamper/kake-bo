package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.matsudamper.money.frontend.common.base.lib.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RootHomeTabPeriodSubCategoryScreen(
    uiState: RootHomeTabPeriodSubCategoryContentUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val savedState = rememberSaveableStateHolder(id = "RootHomeTabPeriodSubCategoryScreen")
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    RootHomeTabScreenScaffold(
        kakeboScaffoldListener = uiState.kakeboScaffoldListener,
        modifier = Modifier.fillMaxSize(),
        content = {
            when (val loadingState = uiState.loadingState) {
                is RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loaded -> {
                    savedState.SaveableStateProvider(Unit) {
                        LoadedContent(
                            modifier = Modifier.fillMaxSize(),
                            loadingState = loadingState,
                        )
                    }
                }

                RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = modifier,
                        onClickRetry = { /* TODO */ },
                    )
                }
            }
        },
        windowInsets = windowInsets,
    )
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    loadingState: RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loaded,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "サブカテゴリ: ${loadingState.subCategoryName}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card {
            BarGraph(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(500.dp),
                uiState = loadingState.graphItems,
                contentColor = LocalContentColor.current,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                loadingState.monthTotalItems.forEach {
                    Row {
                        Text(it.title)
                        Spacer(modifier = Modifier.widthIn(min = 8.dp).weight(1f))
                        Text(it.amount)
                    }
                }
            }
        }
    }
}
