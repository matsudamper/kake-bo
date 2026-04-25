package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.ColorUtil
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.colorpicker.ColorPickerDialog

public data class SettingCategoryScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val categoryName: String,
    val categoryColor: Color?,
    val heroMode: HeroMode,
    val isAddingSubCategory: Boolean,
    val showColorPickerDialog: Boolean,
    val confirmDialog: ConfirmDialog?,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public sealed interface HeroMode {
        public data object Base : HeroMode

        public data object EditingCategoryName : HeroMode
    }

    @Immutable
    public interface ConfirmDialog {
        public val title: String
        public val description: String?

        public fun onConfirm()

        public fun onDismiss()
    }

    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val item: ImmutableList<SubCategoryItem>,
        ) : LoadingState
    }

    public data class SubCategoryItem(
        val id: MoneyUsageSubCategoryId,
        val name: String,
        val isEditing: Boolean,
        val event: Event,
    ) {
        public interface Event {
            public fun onClick()

            public fun onClickDelete()

            public fun onClickEdit()

            public fun onEditComplete(text: String)

            public fun onEditDismiss()
        }
    }

    public interface Event {
        public suspend fun onResume()

        public fun onClickBack()

        public fun onClickEditCategoryName()

        public fun onCategoryNameEditComplete(text: String)

        public fun onCategoryNameEditDismiss()

        public fun onClickChangeColor()

        public fun onDismissColorPicker()

        public fun onColorSelected(color: Color)

        public fun onClickDeleteCategory()

        public fun onClickAddSubCategory()

        public fun onAddSubCategoryComplete(text: String)

        public fun onAddSubCategoryDismiss()
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
                { Text(it) }
            },
            onClickPositive = { confirmDialog.onConfirm() },
            onClickNegative = { confirmDialog.onDismiss() },
            onDismissRequest = { confirmDialog.onDismiss() },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState.loadingState) {
            is SettingCategoryScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is SettingCategoryScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    loadedState = state,
                    windowInsets = windowInsets,
                )
            }
        }
    }
}

@Composable
public fun MainContent(
    modifier: Modifier,
    uiState: SettingCategoryScreenUiState,
) {
    SettingCategoryScreen(
        modifier = modifier,
        uiState = uiState,
        windowInsets = PaddingValues(0.dp),
    )
}

@Composable
private fun LoadedContent(
    modifier: Modifier,
    uiState: SettingCategoryScreenUiState,
    loadedState: SettingCategoryScreenUiState.LoadingState.Loaded,
    windowInsets: PaddingValues,
) {
    val heroColor = uiState.categoryColor ?: MaterialTheme.colorScheme.primary

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeroSection(
                modifier = Modifier.fillMaxWidth(),
                categoryName = uiState.categoryName,
                categoryColor = heroColor,
                heroMode = uiState.heroMode,
                windowInsets = windowInsets,
                onClickBack = { uiState.event.onClickBack() },
                onClickEditCategoryName = { uiState.event.onClickEditCategoryName() },
                onCategoryNameEditComplete = { text -> uiState.event.onCategoryNameEditComplete(text) },
                onCategoryNameEditDismiss = { uiState.event.onCategoryNameEditDismiss() },
                onClickChangeColor = { uiState.event.onClickChangeColor() },
                onClickDeleteCategory = { uiState.event.onClickDeleteCategory() },
            )

            val lazyListState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                item {
                    SubCategoryHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 8.dp),
                        count = loadedState.item.size,
                    )
                }
                if (uiState.isAddingSubCategory) {
                    item {
                        SubCategoryAddRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 8.dp),
                            onComplete = { text -> uiState.event.onAddSubCategoryComplete(text) },
                            onDismiss = { uiState.event.onAddSubCategoryDismiss() },
                        )
                    }
                }
                itemsIndexed(loadedState.item, key = { _, item -> item.id.id }) { index, item ->
                    val position = when {
                        loadedState.item.size == 1 -> RowPosition.Single
                        index == 0 -> RowPosition.First
                        index == loadedState.item.size - 1 -> RowPosition.Last
                        else -> RowPosition.Middle
                    }
                    SubCategoryRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        item = item,
                        position = position,
                        isAddMode = uiState.isAddingSubCategory,
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        if (!uiState.isAddingSubCategory) {
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                onClick = { uiState.event.onClickAddSubCategory() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                text = { Text("サブカテゴリを追加") },
            )
        }
    }
}

@Composable
private fun HeroSection(
    modifier: Modifier,
    categoryName: String,
    categoryColor: Color,
    heroMode: SettingCategoryScreenUiState.HeroMode,
    windowInsets: PaddingValues,
    onClickBack: () -> Unit,
    onClickEditCategoryName: () -> Unit,
    onCategoryNameEditComplete: (String) -> Unit,
    onCategoryNameEditDismiss: () -> Unit,
    onClickChangeColor: () -> Unit,
    onClickDeleteCategory: () -> Unit,
) {
    val isEditMode = heroMode == SettingCategoryScreenUiState.HeroMode.EditingCategoryName
    val topPadding = windowInsets.calculateTopPadding()
    var editingText by rememberSaveable(categoryName, isEditMode) { mutableStateOf(categoryName) }

    Box(
        modifier = modifier
            .background(
                color = categoryColor,
                shape = RectangleShape,
            )
            .padding(top = topPadding),
    ) {
        Column {
            HeroTopBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp),
                isEditMode = isEditMode,
                editingText = editingText,
                onClickBack = onClickBack,
                onCategoryNameEditComplete = onCategoryNameEditComplete,
                onCategoryNameEditDismiss = onCategoryNameEditDismiss,
                onClickDeleteCategory = onClickDeleteCategory,
            )

            HeroBody(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                categoryName = categoryName,
                categoryColor = categoryColor,
                isEditMode = isEditMode,
                editingText = editingText,
                onEditingTextChange = { editingText = it },
                onClickEditCategoryName = onClickEditCategoryName,
                onClickChangeColor = onClickChangeColor,
            )
        }
    }
}

@Composable
private fun HeroTopBar(
    modifier: Modifier,
    isEditMode: Boolean,
    editingText: String,
    onClickBack: () -> Unit,
    onCategoryNameEditComplete: (String) -> Unit,
    onCategoryNameEditDismiss: () -> Unit,
    onClickDeleteCategory: () -> Unit,
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                if (isEditMode) {
                    onCategoryNameEditDismiss()
                } else {
                    onClickBack()
                }
            },
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isEditMode) "キャンセル" else "戻る",
                tint = Color.White,
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = "カテゴリ",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )

        if (isEditMode) {
            TextButton(onClick = { onCategoryNameEditComplete(editingText) }) {
                Text(
                    text = "完了",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "メニュー",
                        tint = Color.White.copy(alpha = 0.9f),
                    )
                }
                if (showMoreMenu) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        onDismissRequest = { showMoreMenu = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        Card(elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showMoreMenu = false
                                            onClickDeleteCategory()
                                        }
                                        .padding(12.dp),
                                    text = "カテゴリを削除",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBody(
    modifier: Modifier,
    categoryName: String,
    categoryColor: Color,
    isEditMode: Boolean,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    onClickEditCategoryName: () -> Unit,
    onClickChangeColor: () -> Unit,
) {
    Column(modifier = modifier) {
        if (isEditMode) {
            Text(
                text = "カテゴリ名",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    modifier = Modifier.weight(1f),
                    value = editingText,
                    onValueChange = onEditingTextChange,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(26.dp)
                        .background(Color.White.copy(alpha = 0.9f)),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                    ),
                    color = Color.White,
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    modifier = Modifier.size(36.dp),
                    onClick = onClickEditCategoryName,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "カテゴリ名を変更",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        ColorBadge(
            color = categoryColor,
            onClick = onClickChangeColor,
        )
    }
}

@Composable
private fun ColorBadge(
    color: Color,
    onClick: () -> Unit,
) {
    val hexCode = "#${ColorUtil.toHexColor(color)}"
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = hexCode,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f),
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SubCategoryHeader(
    modifier: Modifier,
    count: Int,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "サブカテゴリ",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${count}件",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubCategoryAddRow(
    modifier: Modifier,
    onComplete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val accentColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(2.dp, accentColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
            ) {
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { text = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = "名前を入力…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { onComplete(text) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "追加",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "キャンセル",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private enum class RowPosition { Single, First, Middle, Last }

@Composable
private fun SubCategoryRow(
    item: SettingCategoryScreenUiState.SubCategoryItem,
    isAddMode: Boolean,
    position: RowPosition,
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val rowShape = when (position) {
        RowPosition.Single -> RoundedCornerShape(12.dp)
        RowPosition.First -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        RowPosition.Last -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        RowPosition.Middle -> RectangleShape
    }
    val isLast = position == RowPosition.Last || position == RowPosition.Single

    if (item.isEditing) {
        var editingText by remember(item.name) { mutableStateOf(item.name) }

        Row(
            modifier = modifier
                .background(accentColor.copy(alpha = 0.06f), rowShape)
                .clip(rowShape)
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, accentColor, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    modifier = Modifier.weight(1f),
                    value = editingText,
                    onValueChange = { editingText = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .background(accentColor),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { item.event.onEditComplete(editingText) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "確定",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { item.event.onEditDismiss() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "キャンセル",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, rowShape)
                .clip(rowShape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isAddMode) {
                            Modifier.clickable { item.event.onClick() }
                        } else {
                            Modifier
                        },
                    )
                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isAddMode) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (!isAddMode) {
                    IconButton(onClick = { item.event.onClickEdit() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "名前を変更",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!isAddMode) {
                    IconButton(onClick = { item.event.onClickDelete() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "削除",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
@Preview
private fun CategorySettingScreenPreviewContent(
    heroMode: SettingCategoryScreenUiState.HeroMode = SettingCategoryScreenUiState.HeroMode.Base,
    isAddingSubCategory: Boolean = false,
    editingSubCategoryIndex: Int? = null,
) {
    val subCategories = listOf(
        "スーパー",
        "コンビニ",
        "外食",
        "カフェ",
        "お酒",
        "デリバリー",
        "お菓子",
        "パン屋",
    )
    val uiState = SettingCategoryScreenUiState(
        event = object : SettingCategoryScreenUiState.Event {
            override suspend fun onResume() {}
            override fun onClickBack() {}
            override fun onClickEditCategoryName() {}
            override fun onCategoryNameEditComplete(text: String) {}
            override fun onCategoryNameEditDismiss() {}
            override fun onClickChangeColor() {}
            override fun onDismissColorPicker() {}
            override fun onColorSelected(color: Color) {}
            override fun onClickDeleteCategory() {}
            override fun onClickAddSubCategory() {}
            override fun onAddSubCategoryComplete(text: String) {}
            override fun onAddSubCategoryDismiss() {}
        },
        loadingState = SettingCategoryScreenUiState.LoadingState.Loaded(
            item = ImmutableList(
                subCategories.mapIndexed { index, name ->
                    SettingCategoryScreenUiState.SubCategoryItem(
                        id = MoneyUsageSubCategoryId(index),
                        name = name,
                        isEditing = index == editingSubCategoryIndex,
                        event = object : SettingCategoryScreenUiState.SubCategoryItem.Event {
                            override fun onClick() {}
                            override fun onClickDelete() {}
                            override fun onClickEdit() {}
                            override fun onEditComplete(text: String) {}
                            override fun onEditDismiss() {}
                        },
                    )
                },
            ),
        ),
        categoryName = "食費",
        categoryColor = Color(0xFF4F8F2C),
        heroMode = heroMode,
        isAddingSubCategory = isAddingSubCategory,
        showColorPickerDialog = false,
        confirmDialog = null,
        kakeboScaffoldListener = object : KakeboScaffoldListener {
            override fun onClickTitle() {}
        },
    )

    AppRoot {
        SettingCategoryScreen(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxSize(),
            uiState = uiState,
            windowInsets = PaddingValues(0.dp),
        )
    }
}
