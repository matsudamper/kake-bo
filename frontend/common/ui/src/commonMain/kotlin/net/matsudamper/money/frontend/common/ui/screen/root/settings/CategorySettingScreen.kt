package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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
    val defaultColor = presetColors.first()
    var selectedColor by remember {
        mutableStateOf(normalizeHexColor(currentColor) ?: defaultColor)
    }
    var hexInputText by remember {
        mutableStateOf(selectedColor.removePrefix("#"))
    }
    val isHexInputError = hexInputText.isNotEmpty() && normalizeHexColor(hexInputText) == null

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
                CircularColorPicker(
                    selectedColor = parseHexColor(selectedColor),
                    onColorChanged = { color ->
                        val hexColor = color.toHexColorString()
                        selectedColor = hexColor
                        hexInputText = hexColor.removePrefix("#")
                    },
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = hexInputText,
                    onValueChange = { value ->
                        val normalizedText = value
                            .removePrefix("#")
                            .uppercase()
                            .take(6)
                            .filter { it in '0'..'9' || it in 'A'..'F' }
                        hexInputText = normalizedText
                        val normalizedHexColor = normalizeHexColor(normalizedText)
                        if (normalizedHexColor != null) {
                            selectedColor = normalizedHexColor
                        }
                    },
                    label = { Text("HEX (#RRGGBB)") },
                    prefix = { Text("#") },
                    singleLine = true,
                    isError = isHexInputError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
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
                                .clickable {
                                    selectedColor = color
                                    hexInputText = color.removePrefix("#")
                                },
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
                        onClick = { onColorSelected(selectedColor) },
                        enabled = isHexInputError.not(),
                    ) {
                        Text("決定")
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularColorPicker(
    selectedColor: Color,
    onColorChanged: (Color) -> Unit,
) {
    val hueAndSaturation = selectedColor.toHueAndSaturation()
    BoxWithConstraints {
        val canvasSize = minOf(maxWidth, 220.dp)
        val radius = canvasSize / 2
        Canvas(
            modifier = Modifier
                .size(canvasSize)
                .pointerInput(Unit) {
                    detectTapGestures { point ->
                        val color = point.toColorFromWheel(radius.toPx())
                        if (color != null) {
                            onColorChanged(color)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { point ->
                            val color = point.toColorFromWheel(radius.toPx())
                            if (color != null) {
                                onColorChanged(color)
                            }
                        },
                        onDrag = { change, _ ->
                            val color = change.position.toColorFromWheel(radius.toPx())
                            if (color != null) {
                                onColorChanged(color)
                            }
                        },
                    )
                },
        ) {
            val canvasRadius = size.minDimension / 2f
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red,
                    ),
                ),
                radius = canvasRadius,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = canvasRadius,
                ),
                radius = canvasRadius,
            )

            val indicatorDistance = canvasRadius * hueAndSaturation.second
            val indicatorAngle = hueAndSaturation.first * PI.toFloat() / 180f
            val indicatorCenter = Offset(
                x = center.x + cos(indicatorAngle) * indicatorDistance,
                y = center.y + sin(indicatorAngle) * indicatorDistance,
            )

            drawCircle(color = Color.White, radius = 8.dp.toPx(), center = indicatorCenter)
            drawCircle(color = Color.Black, radius = 6.dp.toPx(), center = indicatorCenter)
        }
    }
}

private fun Offset.toColorFromWheel(radius: Float): Color? {
    val centerX = radius
    val centerY = radius
    val dx = x - centerX
    val dy = y - centerY
    val distance = sqrt(dx * dx + dy * dy)
    if (distance > radius) {
        return null
    }
    val hue = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
    val saturation = (distance / radius).coerceIn(0f, 1f)
    return hsvToColor(hue = hue, saturation = saturation, value = 1f)
}

private fun Color.toHueAndSaturation(): Pair<Float, Float> {
    val maxValue = maxOf(red, green, blue)
    val minValue = minOf(red, green, blue)
    val delta = maxValue - minValue

    val hue = when {
        delta == 0f -> 0f
        maxValue == red -> 60f * (((green - blue) / delta) % 6f)
        maxValue == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val saturation = if (maxValue == 0f) {
        0f
    } else {
        delta / maxValue
    }
    return hue to saturation
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val c = value * saturation
    val x = c * (1 - kotlin.math.abs((hue / 60f % 2f) - 1))
    val m = value - c

    val (r1, g1, b1) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = ((r1 + m) * 255).toInt().coerceIn(0, 255),
        green = ((g1 + m) * 255).toInt().coerceIn(0, 255),
        blue = ((b1 + m) * 255).toInt().coerceIn(0, 255),
    )
}

private fun Color.toHexColorString(): String {
    val redValue = (red * 255).toInt().coerceIn(0, 255)
    val greenValue = (green * 255).toInt().coerceIn(0, 255)
    val blueValue = (blue * 255).toInt().coerceIn(0, 255)
    return "#${redValue.toHex2()}${greenValue.toHex2()}${blueValue.toHex2()}"
}

private fun Int.toHex2(): String {
    return toString(radix = 16).uppercase().padStart(2, '0')
}

private fun normalizeHexColor(rawHex: String?): String? {
    if (rawHex.isNullOrBlank()) {
        return null
    }
    val normalizedText = rawHex.removePrefix("#").uppercase()
    if (normalizedText.length != 6) {
        return null
    }
    if (normalizedText.any { it !in '0'..'9' && it !in 'A'..'F' }) {
        return null
    }
    return "#$normalizedText"
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
