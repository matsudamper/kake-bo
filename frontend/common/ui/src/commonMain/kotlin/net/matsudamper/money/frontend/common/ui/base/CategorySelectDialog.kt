package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList

@Composable
internal fun CategorySelectDialog(uiState: CategorySelectDialogUiState) {
    AlertDialog(
        onDismissRequest = { uiState.event.dismissRequest() },
        confirmButton = {
            if (uiState.screenType is CategorySelectDialogUiState.Screen.Root) {
                TextButton(
                    onClick = { uiState.event.selectCompleted() },
                ) {
                    Text("OK")
                }
            }
        },
        dismissButton = if (uiState.screenType is CategorySelectDialogUiState.Screen.Root) {
            {
                TextButton(
                    onClick = { uiState.event.dismissRequest() },
                ) {
                    Text("キャンセル")
                }
            }
        } else {
            null
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
            ) {
                when (val screenTypeState = uiState.screenType) {
                    is CategorySelectDialogUiState.Screen.Root -> {
                        SelectedSection(
                            onClick = { screenTypeState.onClickCategory() },
                            enabled = true,
                            title = {
                                Text(text = "カテゴリ")
                            },
                            description = {
                                Text(text = screenTypeState.category)
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        SelectedSection(
                            onClick = { screenTypeState.onClickSubCategory() },
                            enabled = screenTypeState.enableSubCategory,
                            title = {
                                Text(text = "サブカテゴリ")
                            },
                            description = {
                                Text(text = screenTypeState.subCategory)
                            },
                        )
                    }

                    is CategorySelectDialogUiState.Screen.Category -> {
                        CategoryPage(
                            items = screenTypeState.categories,
                            onBackRequest = { screenTypeState.onBackRequest() },
                            title = {
                                Text(text = "カテゴリ一覧")
                            },
                        )
                    }

                    is CategorySelectDialogUiState.Screen.SubCategory -> {
                        CategoryPage(
                            items = screenTypeState.subCategories,
                            onBackRequest = { screenTypeState.onBackRequest() },
                            title = {
                                Text(text = "サブカテゴリ一覧")
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun CategoryPage(
    modifier: Modifier = Modifier,
    items: ImmutableList<CategorySelectDialogUiState.Category>?,
    title: @Composable () -> Unit,
    onBackRequest: () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onBackRequest() },
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
            }
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                title()
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(Modifier.fillMaxWidth().height(1.dp))

        if (items == null) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .heightIn(min = 200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items, key = { it.id }) { item ->
                    CategoryItem(
                        name = item.name,
                        isSelected = item.isSelected,
                        onSelected = {
                            item.onSelected()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedSection(
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .clickable(enabled) { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            ProvideTextStyle(
                TextStyle(
                    color = if (enabled) {
                        Color.Unspecified
                    } else {
                        Color.Gray
                    },
                ),
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    title()
                }
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    description()
                }
            }
        }
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
        )
    }
}

@Composable
private fun CategoryItem(
    modifier: Modifier = Modifier,
    name: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    Text(
        modifier = modifier.fillMaxWidth()
            .clickable { onSelected() }
            .padding(12.dp),
        text = name,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Unspecified
        },
    )
}
