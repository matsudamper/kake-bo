package net.matsudamper.money.frontend.common.ui.screen.moneyusage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import coil3.compose.AsyncImage
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialog
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffold
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.CalendarDialog
import net.matsudamper.money.frontend.common.ui.layout.GridColumn
import net.matsudamper.money.frontend.common.ui.layout.NumberInput
import net.matsudamper.money.frontend.common.ui.layout.NumberInputValue
import net.matsudamper.money.frontend.common.ui.layout.TimePickerDialog
import net.matsudamper.money.frontend.common.ui.layout.UrlClickableText
import net.matsudamper.money.frontend.common.ui.layout.UrlMenuDialog
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput
import net.matsudamper.money.frontend.common.ui.layout.image.ImageUploadButton
import net.matsudamper.money.frontend.common.ui.layout.image.ZoomableImageDialog

public data class MoneyUsageScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val confirmDialog: ConfirmDialog?,
    val textInputDialog: TextInputDialog?,
    val calendarDialog: CalendarDialog?,
    val timePickerDialog: TimePickerDialogState?,
    val urlMenuDialog: UrlMenuDialog?,
    val numberInputDialog: NumberInputDialog?,
    val categorySelectDialog: CategorySelectDialogUiState?,
) {
    public data class CalendarDialog(
        val date: LocalDate,
        val onSelectedDate: (LocalDate) -> Unit,
        val dismissRequest: () -> Unit,
    )

    public data class TimePickerDialogState(
        val time: LocalTime,
        val onSelectedTime: (LocalTime) -> Unit,
        val dismissRequest: () -> Unit,
    )

    public data class TextInputDialog(
        val isMultiline: Boolean,
        val title: String,
        val onComplete: (String) -> Unit,
        val onCancel: () -> Unit,
        val default: String,
    )

    public data class ConfirmDialog(
        val title: String,
        val description: String?,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit,
    )

    public data class NumberInputDialog(
        val value: NumberInputValue,
        val onChangeValue: (NumberInputValue) -> Unit,
        val dismissRequest: () -> Unit,
    )

    @Immutable
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object Error : LoadingState

        public data class Loaded(
            val moneyUsage: MoneyUsage,
            val linkedMails: ImmutableList<MailItem>,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class MoneyUsage(
        val title: String,
        val description: Clickable,
        val amount: String,
        val category: String,
        val dateTime: String,
        val time: String,
        val images: ImmutableList<ImageItem>,
        val isImageUploading: Boolean,
        val event: MoneyUsageEvent,
    )

    public data class ImageItem(
        val url: String,
        val event: ImageItemEvent,
    )

    @Immutable
    public interface ImageItemEvent {
        public fun onClickDelete()
    }

    public data class MailItem(
        val subject: String,
        val from: String,
        val date: String,
        val event: MailItemEvent,
    )

    @Immutable
    public interface MoneyUsageEvent {
        public fun onClickTitleChange()

        public fun onClickDateChange()

        public fun onClickTimeChange()

        public fun onClickCategoryChange()

        public fun onClickDescription()

        public fun onClickAmountChange()

        public fun onClickUploadImage()
    }

    @Immutable
    public interface MailItemEvent {
        public fun onClick()
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickDelete()

        public fun onClickCopy()
    }

    public data class Clickable(
        val text: String,
        val event: ClickableEvent,
    )

    @Immutable
    public interface ClickableEvent {
        public fun onClickUrl(url: String)

        public fun onLongClickUrl(text: String)
    }

    public data class UrlMenuDialog(
        val url: String,
        val event: UrlMenuDialogEvent,
    )

    @Immutable
    public interface UrlMenuDialogEvent {
        public fun onClickOpen()

        public fun onClickCopy()

        public fun onDismissRequest()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()

        public fun onClickRetry()

        public fun onClickBack()
    }
}

@Composable
public fun MoneyUsageScreen(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    if (uiState.urlMenuDialog != null) {
        UrlMenuDialog(
            url = uiState.urlMenuDialog.url,
            onClickOpen = { uiState.urlMenuDialog.event.onClickOpen() },
            onClickCopy = { uiState.urlMenuDialog.event.onClickCopy() },
            onDismissRequest = { uiState.urlMenuDialog.event.onDismissRequest() },
        )
    }
    if (uiState.confirmDialog != null) {
        AlertDialog(
            title = { Text(uiState.confirmDialog.title) },
            onDismissRequest = { uiState.confirmDialog.onDismiss() },
            description = uiState.confirmDialog.description?.let { description -> @Composable { Text(description) } },
            onClickNegative = { uiState.confirmDialog.onDismiss() },
            onClickPositive = { uiState.confirmDialog.onConfirm() },
        )
    }
    if (uiState.textInputDialog != null) {
        FullScreenTextInput(
            isMultiline = uiState.textInputDialog.isMultiline,
            title = uiState.textInputDialog.title,
            default = uiState.textInputDialog.default,
            onComplete = { uiState.textInputDialog.onComplete(it) },
            canceled = { uiState.textInputDialog.onCancel() },
        )
    }
    if (uiState.calendarDialog != null) {
        CalendarDialog(
            initialCalendar = uiState.calendarDialog.date,
            dismissRequest = {
                uiState.calendarDialog.dismissRequest()
            },
            selectedCalendar = {
                uiState.calendarDialog.onSelectedDate(it)
            },
        )
    }
    if (uiState.timePickerDialog != null) {
        TimePickerDialog(
            initialTime = uiState.timePickerDialog.time,
            dismissRequest = {
                uiState.timePickerDialog.dismissRequest()
            },
            selectedTime = {
                uiState.timePickerDialog.onSelectedTime(it)
            },
        )
    }
    if (uiState.categorySelectDialog != null) {
        CategorySelectDialog(
            uiState = uiState.categorySelectDialog,
        )
    }
    if (uiState.numberInputDialog != null) {
        Dialog(onDismissRequest = { uiState.numberInputDialog.dismissRequest() }) {
            NumberInput(
                value = uiState.numberInputDialog.value,
                onChangeValue = { uiState.numberInputDialog.onChangeValue(it) },
                dismissRequest = { uiState.numberInputDialog.dismissRequest() },
            )
        }
    }

    KakeboScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) { paddingValues ->
        when (val state = uiState.loadingState) {
            is MoneyUsageScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier
                        .fillMaxWidth(),
                    uiState = state,
                    paddingValues = paddingValues,
                )
            }

            is MoneyUsageScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            MoneyUsageScreenUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier.fillMaxWidth().padding(paddingValues),
                    onClickRetry = { uiState.event.onClickRetry() },
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier,
    uiState: MoneyUsageScreenUiState.LoadingState.Loaded,
    paddingValues: PaddingValues,
) {
    val state = rememberLazyListState()
    BoxWithConstraints(
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            state = state,
            contentPadding = paddingValues,
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.padding(12.dp)
                            .weight(1f),
                        text = "使用用途",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Box {
                        var visiblePopup by remember { mutableStateOf(false) }
                        IconButton(onClick = { visiblePopup = !visiblePopup }) {
                            Icon(Icons.Default.MoreVert, "メニュー")
                        }
                        if (visiblePopup) {
                            UsageMenuPopup(
                                onDismissRequest = { visiblePopup = false },
                                onClickDelete = {
                                    visiblePopup = false
                                    uiState.event.onClickDelete()
                                },
                                onClickCopy = {
                                    visiblePopup = false
                                    uiState.event.onClickCopy()
                                },
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))

                Card(
                    modifier = Modifier
                        .padding(12.dp),
                ) {
                    MoneyUsage(
                        modifier = Modifier.fillMaxWidth()
                            .padding(12.dp),
                        uiState = uiState.moneyUsage,
                    )
                }
            }
            item {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = "連携されたメール",
                    style = MaterialTheme.typography.titleLarge,
                )
                HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
            }
            if (uiState.linkedMails.isNotEmpty()) {
                items(uiState.linkedMails) { mail ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .padding(8.dp),
                        onClick = { mail.event.onClick() },
                    ) {
                        MailContent(
                            modifier = Modifier.fillMaxWidth()
                                .padding(12.dp),
                            uiState = mail,
                        )
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("連携されたメールがありません")
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageMenuPopup(
    onDismissRequest: () -> Unit,
    onClickDelete: () -> Unit,
    onClickCopy: () -> Unit,
) {
    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Card(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp,
            ),
        ) {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            onClickCopy()
                        }
                        .padding(12.dp),
                    text = "コピー",
                )
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            onClickDelete()
                        }
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    text = "削除",
                )
            }
        }
    }
}

@Composable
private fun MailContent(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState.MailItem,
) {
    GridColumn(
        modifier = modifier,
        horizontalPadding = 8.dp,
        verticalPadding = 4.dp,
    ) {
        row {
            item {
                Text(text = "タイトル")
            }
            item {
                Text(text = uiState.subject)
            }
        }
        row {
            item {
                Text(text = "From")
            }
            item {
                Text(text = uiState.from)
            }
        }
        row {
            item {
                Text(text = "日付")
            }
            item {
                Text(text = uiState.date)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoneyUsage(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState.MoneyUsage,
) {
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        MoneyUsageSection(
            title = {
                Text("タイトル")
            },
            content = {
                Text(
                    text = uiState.title,
                )
            },
            onClickChange = {
                uiState.event.onClickTitleChange()
            },
        )
        MoneyUsageSection(
            title = {
                Text("日付")
            },
            content = {
                Text(
                    text = uiState.dateTime,
                )
            },
            onClickChange = {
                uiState.event.onClickDateChange()
            },
        )
        MoneyUsageSection(
            title = {
                Text("時間")
            },
            content = {
                Text(
                    text = uiState.time,
                )
            },
            onClickChange = {
                uiState.event.onClickTimeChange()
            },
        )
        MoneyUsageSection(
            title = {
                Text("カテゴリ")
            },
            content = {
                Text(
                    text = uiState.category,
                )
            },
            onClickChange = {
                uiState.event.onClickCategoryChange()
            },
        )
        MoneyUsageSection(
            title = {
                Text("金額")
            },
            content = {
                Text(
                    text = uiState.amount,
                )
            },
            onClickChange = {
                uiState.event.onClickAmountChange()
            },
        )
        MoneyUsageSection(
            multiline = true,
            title = {
                Text("説明")
            },
            content = {
                UrlClickableText(
                    text = uiState.description.text,
                    onClickUrl = { uiState.description.event.onClickUrl(it) },
                    onLongClickUrl = { uiState.description.event.onLongClickUrl(it) },
                )
            },
            onClickChange = {
                uiState.event.onClickDescription()
            },
        )
        MoneyUsageSection(
            multiline = true,
            showChangeButton = false,
            title = {
                Text("画像")
            },
            content = {
                Column {
                    if (uiState.images.isEmpty() && !uiState.isImageUploading) {
                        Text("未設定")
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.images.forEach { imageItem ->
                                var showDeleteDialog by remember { mutableStateOf(false) }
                                var showPopupMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.size(180.dp)) {
                                    AsyncImage(
                                        model = imageItem.url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { selectedImageUrl = imageItem.url }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = { selectedImageUrl = imageItem.url },
                                                    onLongPress = { showPopupMenu = true },
                                                )
                                            }
                                            .pointerInput(Unit) {
                                                awaitEachGesture {
                                                    val pressEvent = awaitPointerEvent()
                                                    if (pressEvent.type != PointerEventType.Press) return@awaitEachGesture
                                                    if (!pressEvent.buttons.isSecondaryPressed) return@awaitEachGesture

                                                    val releaseEvent = awaitPointerEvent()
                                                    if (releaseEvent.type != PointerEventType.Release) return@awaitEachGesture
                                                    if (pressEvent.buttons.isSecondaryPressed) return@awaitEachGesture

                                                    showPopupMenu = true
                                                }
                                            },
                                    )
                                }
                                DropdownMenu(
                                    expanded = showPopupMenu,
                                    onDismissRequest = { showPopupMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("削除") },
                                        onClick = {
                                            imageItem.event.onClickDelete()
                                        },
                                    )
                                }
                                if (showDeleteDialog) {
                                    AlertDialog(
                                        title = { Text("画像を削除しますか？") },
                                        description = { Text("この操作は取り消せません。") },
                                        positiveButton = { Text("削除") },
                                        negativeButton = { Text("キャンセル") },
                                        onClickPositive = {
                                        },
                                        onClickNegative = { showDeleteDialog = false },
                                        onDismissRequest = { showDeleteDialog = false },
                                    )
                                }
                            }

                            if (uiState.isImageUploading) {
                                Box(
                                    modifier = Modifier.size(180.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ImageUploadButton(
                        onClick = { uiState.event.onClickUploadImage() },
                    )
                }
            },
            onClickChange = {},
        )
    }

    selectedImageUrl?.let { imageUrl ->
        ZoomableImageDialog(
            imageUrl = imageUrl,
            onDismissRequest = { selectedImageUrl = null },
        )
    }
}

@Composable
private fun MoneyUsageSection(
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    showChangeButton: Boolean = true,
    title: @Composable () -> Unit,
    onClickChange: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium) {
            Box(modifier = Modifier.padding(8.dp)) {
                title()
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
        Row(
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                Modifier.weight(1f)
                    .align(
                        if (multiline) {
                            Alignment.Top
                        } else {
                            Alignment.CenterVertically
                        },
                    ),
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    content()
                }
            }

            if (showChangeButton) {
                OutlinedButton(
                    modifier = Modifier.align(
                        if (multiline) {
                            Alignment.Bottom
                        } else {
                            Alignment.CenterVertically
                        },
                    ),
                    onClick = {
                        onClickChange()
                    },
                ) {
                    Text("変更")
                }
            }
        }
    }
}
