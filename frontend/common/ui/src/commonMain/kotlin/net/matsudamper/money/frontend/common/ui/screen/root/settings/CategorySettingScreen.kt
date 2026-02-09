package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput

public data class SettingCategoryScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val showCategoryNameInput: Boolean,
    val categoryName: String,
    val categoryColor: String?,
    val showCategoryNameChangeDialog: FullScreenInputDialog?,
    val showSubCategoryNameChangeDialog: FullScreenInputDialog?,
    val showColorPickerDialog: Boolean,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public data class FullScreenInputDialog(
        val initText: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onDismiss()

            public fun onTextInputCompleted(text: String)
        }
    }

    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val item: ImmutableList<SubCategoryItem>,
        ) : LoadingState
    }

    public data class SubCategoryItem(
        val name: String,
        val event: Event,
    ) {
        public interface Event {
            public fun onClick()

            public fun onClickDelete()

            public fun onClickChangeName()
        }
    }

    public interface Event {
        public suspend fun onResume()

        public fun onClickAddSubCategoryButton()

        public fun subCategoryNameInputCompleted(text: String)

        public fun dismissCategoryInput()

        public fun onClickChangeCategoryName()

        public fun onClickChangeColor()

        public fun onDismissColorPicker()

        public fun onColorSelected(hexColor: String)
    }
}

@Composable
public fun SettingCategoryScreen(
    modifier: Modifier = Modifier,
    uiState: SettingCategoryScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    if (uiState.showCategoryNameInput) {
        FullScreenTextInput(
            title = "サブカテゴリー名",
            onComplete = { text ->
                uiState.event.subCategoryNameInputCompleted(text)
            },
            canceled = {
                uiState.event.dismissCategoryInput()
            },
            default = "",
        )
    }

    if (uiState.showCategoryNameChangeDialog != null) {
        FullScreenTextInput(
            title = "カテゴリー名変更",
            onComplete = { text ->
                uiState.showCategoryNameChangeDialog.event.onTextInputCompleted(text)
            },
            canceled = {
                uiState.showCategoryNameChangeDialog.event.onDismiss()
            },
            default = uiState.showCategoryNameChangeDialog.initText,
        )
    }
    if (uiState.showSubCategoryNameChangeDialog != null) {
        FullScreenTextInput(
            title = "サブカテゴリー名変更",
            onComplete = { text ->
                uiState.showSubCategoryNameChangeDialog.event.onTextInputCompleted(text)
            },
            canceled = {
                uiState.showSubCategoryNameChangeDialog.event.onDismiss()
            },
            default = uiState.showSubCategoryNameChangeDialog.initText,
        )
    }

    if (uiState.showColorPickerDialog) {
        ColorPickerDialog(
            currentColor = uiState.categoryColor,
            onDismiss = { uiState.event.onDismissColorPicker() },
            onColorSelected = { hexColor -> uiState.event.onColorSelected(hexColor) },
        )
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
        windowInsets = windowInsets,
        content = {
            MainContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
            )
        },
    )
}

@Composable
public fun MainContent(
    modifier: Modifier,
    uiState: SettingCategoryScreenUiState,
) {
    SettingScaffold(
        modifier = modifier.fillMaxSize(),
        title = {
            Text(
                text = "カテゴリー設定",
            )
        },
    ) { paddingValues ->
        when (val state = uiState.loadingState) {
            is SettingCategoryScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.fillMaxSize(),
                    paddingValues = paddingValues,
                    loadedState = state,
                    uiState = uiState,
                )
            }

            is SettingCategoryScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    paddingValues: PaddingValues,
    uiState: SettingCategoryScreenUiState,
    loadedState: SettingCategoryScreenUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints {
        val lazyListState = rememberLazyListState()
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                bottom = 0.dp,
            ),
            state = lazyListState,
        ) {
            item {
                Spacer(Modifier.height(24.dp))
            }
            item {
                HeaderSection(
                    modifier = Modifier.fillMaxWidth(),
                    categoryName = uiState.categoryName,
                    categoryColor = uiState.categoryColor,
                    onClickChangeCategoryNameButton = {
                        uiState.event.onClickChangeCategoryName()
                    },
                    onClickChangeColorButton = {
                        uiState.event.onClickChangeColor()
                    },
                    oonClickSubCategoryButton = {
                        uiState.event.onClickAddSubCategoryButton()
                    },
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
            }
            items(
                items = loadedState.item,
            ) { item ->
                SubCategoryItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    item = item,
                )
            }
            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SubCategoryItem(
    modifier: Modifier = Modifier,
    item: SettingCategoryScreenUiState.SubCategoryItem,
) {
    Card(
        modifier = modifier,
        onClick = { item.event.onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = 24.dp,
                        vertical = 24.dp,
                    ),
                text = item.name,
            )
            Spacer(Modifier.weight(1f))
            var showMenu by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    showMenu = true
                },
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "open menu",
                )
            }
            if (showMenu) {
                Popup(
                    alignment = Alignment.CenterEnd,
                    onDismissRequest = {
                        showMenu = false
                    },
                    properties = PopupProperties(focusable = true),
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.width(IntrinsicSize.Min),
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { item.event.onClickChangeName() }
                                    .padding(12.dp),
                                text = "名前変更",
                            )
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { item.event.onClickDelete() }
                                    .padding(12.dp),
                                text = "削除",
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderSection(
    modifier: Modifier,
    categoryName: String,
    categoryColor: String?,
    onClickChangeCategoryNameButton: () -> Unit,
    onClickChangeColorButton: () -> Unit,
    oonClickSubCategoryButton: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlowRow(
            verticalArrangement = Arrangement.Center,
        ) {
            if (categoryColor != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(categoryColor)),
                )
            }
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { onClickChangeCategoryNameButton() },
                ) {
                    Text(text = "カテゴリー名変更")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { onClickChangeColorButton() },
                ) {
                    Text(text = "色変更")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { oonClickSubCategoryButton() },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(text = "サブカテゴリーを追加")
                }
            }
        }
    }
}

private val presetColors: List<String> = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
    "#795548", "#9E9E9E", "#607D8B", "#000000",
)

@Composable
private fun ColorPickerDialog(
    currentColor: String?,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit,
) {
    var selectedColor by remember { mutableStateOf(currentColor) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = "カテゴリーの色を選択",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp),
                ) {
                    items(presetColors) { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(color))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { selectedColor = color },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val color = selectedColor
                            if (color != null) {
                                onColorSelected(color)
                            }
                        },
                        enabled = selectedColor != null,
                    ) {
                        Text("決定")
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    val colorString = hex.removePrefix("#")
    val colorLong = colorString.toLongOrNull(16) ?: return Color.Gray
    return Color(
        red = ((colorLong shr 16) and 0xFF).toInt(),
        green = ((colorLong shr 8) and 0xFF).toInt(),
        blue = (colorLong and 0xFF).toInt(),
    )
}
