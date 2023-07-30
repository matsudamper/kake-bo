package net.matsudamper.money.frontend.common.ui.screen.add_money_usage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList

public data class AddMoneyUsageScreenCategorySelectDialogUiState(
    val screenType: Screen,
    val event: Event,
) {
    public sealed interface Screen {
        public data class Root(
            val category: String,
            val subCategory: String,
            val onClickCategory: () -> Unit,
            val onClickSubCategory: () -> Unit,
        ) : Screen

        public data class Category(
            val categories: ImmutableList<AddMoneyUsageScreenCategorySelectDialogUiState.Category>,
            val onBackRequest: () -> Unit,
        ) : Screen

        public data class SubCategory(
            val subCategories: ImmutableList<AddMoneyUsageScreenCategorySelectDialogUiState.Category>?,
            val onBackRequest: () -> Unit,
        ) : Screen
    }

    public data class Category(
        val name: String,
        val isSelected: Boolean,
        val onSelected: () -> Unit,
    )

    @Immutable
    public interface Event {
        public fun dismissRequest()
        public fun selectCompleted()
    }
}

@Composable
internal fun CategorySelectDialog(
    uiState: AddMoneyUsageScreenCategorySelectDialogUiState,
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { uiState.event.dismissRequest() },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    // カード内をタップしても閉じないようにする
                }
                .widthIn(max = 400.dp)
                .heightIn(max = 700.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
            ) {
                when (val screenTypeState = uiState.screenType) {
                    is AddMoneyUsageScreenCategorySelectDialogUiState.Screen.Root -> {
                        SelectedSection(
                            onClick = { screenTypeState.onClickCategory() },
                            title = {
                                Text(text = "カテゴリ")
                            },
                            description = {
                                Text(text = screenTypeState.category)
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        SelectedSection(
                            onClick = { screenTypeState.onClickSubCategory() },
                            title = {
                                Text(text = "サブカテゴリ")
                            },
                            description = {
                                Text(text = screenTypeState.subCategory)
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.align(Alignment.End)) {
                            TextButton(
                                onClick = { uiState.event.dismissRequest() },
                            ) {
                                Text("キャンセル")
                            }
                            TextButton(
                                onClick = { uiState.event.selectCompleted() },
                            ) {
                                Text("OK")
                            }
                        }
                    }

                    is AddMoneyUsageScreenCategorySelectDialogUiState.Screen.Category -> {
                        CategoryPage(
                            items = screenTypeState.categories,
                            onBackRequest = { screenTypeState.onBackRequest() },
                            title = {
                                Text(text = "カテゴリ一覧")
                            },
                        )
                    }

                    is AddMoneyUsageScreenCategorySelectDialogUiState.Screen.SubCategory -> {
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
        }
    }
}

@Composable
private fun CategoryPage(
    modifier: Modifier = Modifier,
    items: ImmutableList<AddMoneyUsageScreenCategorySelectDialogUiState.Category>?,
    title: @Composable () -> Unit,
    onBackRequest: () -> Unit,
) {
    if (items == null) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 200.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = modifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onBackRequest() },
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "戻る")
                }
                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                    title()
                }
            }
            Spacer(Modifier.height(12.dp))
            Divider(Modifier.fillMaxWidth().height(1.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items) { item ->
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
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                title()
            }
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                description()
            }
        }
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null
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
