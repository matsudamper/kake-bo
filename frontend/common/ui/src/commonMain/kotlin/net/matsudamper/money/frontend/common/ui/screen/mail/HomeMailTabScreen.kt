package net.matsudamper.money.frontend.common.ui.screen.mail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.GridColumn

public data class MailScreenUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public object Error : LoadingState
        public data class Loaded(
            val mail: Mail,
            val usageSuggest: ImmutableList<UsageSuggest>,
        ) : LoadingState
    }

    public data class Mail(
        val from: String,
        val title: String,
        val date: String,
    )

    public data class UsageSuggest(
        val title: String,
        val amount: String?,
        val category: String?,
        val description: String,
        val dateTime: String?,
    )

    @Immutable
    public interface Event {
        public fun onClickRetry()
        public fun onClickArrowBackButton()
        public fun onClickTitle()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MailScreen(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            KakeBoTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { uiState.event.onClickArrowBackButton() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                onClickTitle = {
                    uiState.event.onClickTitle()
                }
            ) {
                Text(
                    text = "家計簿 - メール",
                )
            }
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
    Box(
        modifier = modifier.fillMaxSize()
            .padding(horizontal = 24.dp)
            .widthIn(max = 700.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    text = "メール",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
                Spacer(modifier = Modifier.height(12.dp))
                MailCard(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.mail,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    text = "解析結果",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn {
                    items(uiState.usageSuggest) { item ->
                        MoneyUsageSuggestCard(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 12.dp),
                            items = item,
                        )
                    }
                }
            }
        }
    }
}

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
                        Text("日付")
                    }
                    item {
                        Text(text = items.dateTime.orEmpty())
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
                        Text("カテゴリ")
                    }
                    item {
                        Text(text = items.category.orEmpty())
                    }
                }
                row {
                    item {
                        Text("説明")
                    }
                    item {
                        Text(text = items.description)
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
