package net.matsudamper.money.frontend.feature.notification.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold

@Composable
public fun NotificationUsageListScreen(
    uiState: NotificationUsageListScreenUiState,
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
                menu = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uiState.topBarActions.forEach { action ->
                            TextButton(onClick = { action.listener.onClick() }) {
                                Text(action.label)
                            }
                        }
                    }
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
            uiState.accessSection?.let { accessSection ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = accessSection.title,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = accessSection.description,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            accessSection.buttonLabel?.let { buttonLabel ->
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(
                                    onClick = { accessSection.listener?.onClickButton() },
                                ) {
                                    Text(buttonLabel)
                                }
                            }
                        }
                    }
                }
            }
            if (uiState.filters.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        uiState.filters.forEachIndexed { index, filter ->
                            FilterChip(
                                selected = filter.selected,
                                onClick = { filter.listener.onClick() },
                                label = {
                                    Text(filter.label)
                                },
                            )
                            if (index != uiState.filters.lastIndex) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }
            if (uiState.searchListener != null) {
                item {
                    val searchListener = uiState.searchListener
                    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            searchListener.onSearchQueryChange(newValue.text)
                        },
                        placeholder = { Text("検索") },
                        singleLine = true,
                    )
                }
            }
            when (val itemsState = uiState.itemsState) {
                NotificationUsageListScreenUiState.ItemsState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is NotificationUsageListScreenUiState.ItemsState.Loaded -> {
                    if (itemsState.items.isEmpty()) {
                        item {
                            Text(
                                text = itemsState.emptyText,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        items(itemsState.items) { item ->
                            var showMenu by remember(item) { mutableStateOf(false) }
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                // Jetbrains Compose に combinedClickable がないため、clickable + semantics(customActions) での修正が必要。
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (item.listener != null) {
                                                Modifier.pointerInput(item) {
                                                    detectTapGestures(
                                                        onTap = {
                                                            item.listener.onClick()
                                                        },
                                                        onLongPress = {
                                                            showMenu = true
                                                        },
                                                    )
                                                }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                modifier = Modifier.weight(1f),
                                                text = item.title,
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = item.receivedAt,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.statusLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("JSONでコピー") },
                                        onClick = {
                                            showMenu = false
                                            item.listener?.onClickCopyJson()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
