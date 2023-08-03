package net.matsudamper.money.frontend.common.ui.screen.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class RootListScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public data class Loaded(
            val loadToEnd: Boolean,
            val items: List<Item>,
        ) : LoadingState
    }

    public data class Item(
        val title: String,
        val date: String,
        val amount: String,
        val category: String?,
        val description: String,
    )

    @Immutable
    public interface Event {
        public fun onClickAdd()
        public fun loadMore()
        public fun onViewInitialized()
    }
}

@Composable
public fun RootListScreen(
    modifier: Modifier = Modifier,
    uiState: RootListScreenUiState,
    listener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.List,
        listener = listener,
        content = {
            Box(Modifier.fillMaxSize()) {
                when (uiState.loadingState) {
                    is RootListScreenUiState.LoadingState.Loaded -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(uiState.loadingState.items) { item ->
                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp,
                                        ),
                                    item = item,
                                )
                            }
                            if (uiState.loadingState.loadToEnd.not()) {
                                item {
                                    LaunchedEffect(Unit) {
                                        uiState.event.loadMore()
                                        while (isActive) {
                                            delay(500)
                                            uiState.event.loadMore()
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is RootListScreenUiState.LoadingState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                FloatingActionButton(
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(bottom = 24.dp, end = 24.dp),
                    onClick = { uiState.event.onClickAdd() },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "add money usage",
                    )
                }
            }
        },
    )
}

@Composable
private fun ListItem(
    modifier: Modifier = Modifier,
    item: RootListScreenUiState.Item,
) {
    Card(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.title)
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.description)
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.date)
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.amount)
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.description)
        }
    }
}
