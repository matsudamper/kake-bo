package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.colorpicker.ColorPickerDialog
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput

public data class SettingCategoryScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val showCategoryNameInput: Boolean,
    val categoryName: String,
    val categoryColor: Color?,
    val showCategoryNameChangeDialog: FullScreenInputDialog?,
    val showSubCategoryNameChangeDialog: FullScreenInputDialog?,
    val showColorPickerDialog: Boolean,
    val confirmDialog: ConfirmDialog?,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    @Immutable
    public interface ConfirmDialog {
        public val title: String
        public val description: String?

        public fun onConfirm()

        public fun onDismiss()
    }

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

        public fun onColorSelected(color: Color)

        public fun onClickDeleteCategory()
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
            onColorSelected = { color -> uiState.event.onColorSelected(color) },
        )
    }

    uiState.confirmDialog?.also { confirmDialog ->
        AlertDialog(
            title = { Text(confirmDialog.title) },
            description = confirmDialog.description?.let {
                {
                    Text(it)
                }
            },
            onClickPositive = { confirmDialog.onConfirm() },
            onClickNegative = { confirmDialog.onDismiss() },
            onDismissRequest = { confirmDialog.onDismiss() },
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
                    onClickSubCategoryButton = {
                        uiState.event.onClickAddSubCategoryButton()
                    },
                    onClickDeleteCategoryButton = {
                        uiState.event.onClickDeleteCategory()
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
private fun HeaderSection(
    modifier: Modifier,
    categoryName: String,
    categoryColor: Color?,
    onClickChangeCategoryNameButton: () -> Unit,
    onClickChangeColorButton: () -> Unit,
    onClickSubCategoryButton: () -> Unit,
    onClickDeleteCategoryButton: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (categoryColor != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(categoryColor),
                    )
                }
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                var showCategoryMenu by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showCategoryMenu = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "カテゴリ操作メニューを開く",
                    )
                }
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("カテゴリー名変更") },
                        onClick = {
                            showCategoryMenu = false
                            onClickChangeCategoryNameButton()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("色変更") },
                        onClick = {
                            showCategoryMenu = false
                            onClickChangeColorButton()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("カテゴリーを削除") },
                        onClick = {
                            showCategoryMenu = false
                            onClickDeleteCategoryButton()
                        },
                    )
                }
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onClickSubCategoryButton() },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "サブカテゴリーを追加")
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
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = {
                    showMenu = false
                },
            ) {
                DropdownMenuItem(
                    text = { Text("名前変更") },
                    onClick = {
                        showMenu = false
                        item.event.onClickChangeName()
                    },
                )
                DropdownMenuItem(
                    text = { Text("削除") },
                    onClick = {
                        showMenu = false
                        item.event.onClickDelete()
                    },
                )
            }
        }
    }
}
