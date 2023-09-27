package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent

public data class RootHomeTabPeriodUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {

        public data class Loaded(
            val rangeText: String,
            val between: String,
            val categoryType: String,
            val categoryTypes: ImmutableList<CategoryTypes>,
        ) : LoadingState

        public data object Loading : LoadingState
        public data object Error : LoadingState
    }

    public data class CategoryTypes(
        val title: String,
        val onClick: () -> Unit,
    )

    public data class MonthTotalItem(
        val title: String,
        val amount: String,
    )

    @Immutable
    public interface Event {
        public fun onClickNextMonth()
        public fun onClickPreviousMonth()
        public fun onClickRange(range: Int)
        public fun onViewInitialized()
        public fun onClickRetry()
    }
}

@Composable
public fun RootHomeTabPeriodScaffold(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodUiState,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    BoxWithConstraints(modifier = modifier) {
        when (uiState.loadingState) {
            RootHomeTabPeriodUiState.LoadingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            RootHomeTabPeriodUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier.fillMaxWidth(),
                    onClickRetry = {
                        uiState.event.onClickRetry()
                    },
                )
            }

            is RootHomeTabPeriodUiState.LoadingState.Loaded -> {
                val height = this.maxHeight
                val scrollState = rememberScrollState()
                val density = LocalDensity.current
                var scrollBarHeight by remember { mutableIntStateOf(0) }
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                ) {
                    RootHomePeriodSection(
                        modifier = Modifier.fillMaxWidth(),
                        onClickPreviousMonth = { uiState.event.onClickPreviousMonth() },
                        onClickNextMonth = { uiState.event.onClickNextMonth() },
                        betweenText = {
                            Text(uiState.loadingState.between)
                        },
                        rangeText = {
                            Text(uiState.loadingState.rangeText)
                        },
                        onClickRange = { range -> uiState.event.onClickRange(range) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BetweenLoaded(
                        uiState = uiState.loadingState,
                        content = content,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Spacer(modifier = Modifier.height(with(density) { scrollBarHeight.toDp() }))
                }
                ScrollButtons(
                    modifier = Modifier
                        .onSizeChanged {
                            scrollBarHeight = it.height
                        }
                        .align(Alignment.BottomEnd)
                        .padding(ScrollButtonsDefaults.padding)
                        .height(ScrollButtonsDefaults.height),
                    scrollState = scrollState,
                    scrollSize = with(density) {
                        height.toPx() * 0.4f
                    },
                )
            }
        }
    }
}

@Composable
private fun BetweenLoaded(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodUiState.LoadingState.Loaded,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 4.dp),
        ) {
            var expanded by remember { mutableStateOf(false) }
            DropDownMenuButton(
                modifier = Modifier,
                onClick = { expanded = !expanded },
            ) {
                Text(uiState.categoryType)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                uiState.categoryTypes.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(type.title)
                        },
                        onClick = { type.onClick() },
                    )
                }
            }
        }
        content()
    }
}
