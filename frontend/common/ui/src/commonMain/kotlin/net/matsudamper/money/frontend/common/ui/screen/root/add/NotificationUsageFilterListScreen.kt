package net.matsudamper.money.frontend.common.ui.screen.root.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold

public data class NotificationUsageFilterListScreenUiState(
    val title: String,
    val description: String,
    val filters: ImmutableList<FilterItem>,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public data class FilterItem(
        val title: String,
        val matchDescription: String,
        val parseDescription: String,
        val autoAddEnabled: Boolean,
        val onToggleAutoAdd: (Boolean) -> Unit,
    )
}

@Composable
public fun NotificationUsageFilterListScreen(
    uiState: NotificationUsageFilterListScreenUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    RootScreenScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable { uiState.kakeboScaffoldListener.onClickTitle() },
                        text = uiState.title,
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.filters.isEmpty()) {
                item {
                    Text(
                        text = "利用できる通知フィルターはありません。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(uiState.filters) { filter ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = filter.title,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = "条件: ${filter.matchDescription}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = filter.autoAddEnabled,
                                    onCheckedChange = filter.onToggleAutoAdd,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "自動追加: ${if (filter.autoAddEnabled) "ON" else "OFF"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "変換: ${filter.parseDescription}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
