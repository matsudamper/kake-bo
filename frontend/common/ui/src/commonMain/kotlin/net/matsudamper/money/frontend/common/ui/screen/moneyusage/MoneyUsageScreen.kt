package net.matsudamper.money.frontend.common.ui.screen.moneyusage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import coil3.compose.SubcomposeAsyncImage
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.LocalIsLargeScreen
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialog
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffold
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_email
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_image
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_more_vert
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.CalendarDialog
import net.matsudamper.money.frontend.common.ui.layout.NumberInput
import net.matsudamper.money.frontend.common.ui.layout.NumberInputValue
import net.matsudamper.money.frontend.common.ui.layout.TimePickerDialog
import net.matsudamper.money.frontend.common.ui.layout.UrlClickableText
import net.matsudamper.money.frontend.common.ui.layout.UrlMenuDialog
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput
import net.matsudamper.money.frontend.common.ui.layout.image.ImageLoadingPlaceholder
import net.matsudamper.money.frontend.common.ui.layout.image.ImageUploadButton
import net.matsudamper.money.frontend.common.ui.layout.image.ZoomableImageDialog
import org.jetbrains.compose.resources.painterResource

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
        val date: String,
        val time: String,
        val images: ImmutableList<ImageItem>,
        val uploadQueueItems: ImmutableList<UploadQueueItem>,
        val event: MoneyUsageEvent,
    )

    public data class UploadQueueItem(
        val id: String,
        val previewBytes: ByteArray?,
        val isLoading: Boolean,
        val isFailed: Boolean,
        val onClickRetry: () -> Unit,
        val onLongPressCancel: () -> Unit,
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
                        Icon(painter = painterResource(Res.drawable.ic_arrow_back), contentDescription = "戻る")
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
                        text = "使用用途",
                    )
                },
                menu = {
                    var visiblePopup by remember { mutableStateOf(false) }
                    IconButton(onClick = { visiblePopup = !visiblePopup }) {
                        Icon(painter = painterResource(Res.drawable.ic_more_vert), contentDescription = "メニュー")
                    }
                    if (visiblePopup) {
                        UsageMenuPopup(
                            onDismissRequest = { visiblePopup = false },
                            onClickDelete = {
                                visiblePopup = false
                                if (uiState.loadingState is MoneyUsageScreenUiState.LoadingState.Loaded) {
                                    uiState.loadingState.event.onClickDelete()
                                }
                            },
                            onClickCopy = {
                                visiblePopup = false
                                if (uiState.loadingState is MoneyUsageScreenUiState.LoadingState.Loaded) {
                                    uiState.loadingState.event.onClickCopy()
                                }
                            },
                        )
                    }
                },
                windowInsets = windowInsets,
            )
        },
    ) { paddingValues ->
        when (val state = uiState.loadingState) {
            is MoneyUsageScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.fillMaxWidth(),
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
    val isLargeScreen = LocalIsLargeScreen.current
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().padding(paddingValues),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isLargeScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        MainCard(uiState = uiState.moneyUsage)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ImagesCard(uiState = uiState.moneyUsage)
                        MailsSection(
                            linkedMails = uiState.linkedMails,
                        )
                    }
                }
            } else {
                MainCard(uiState = uiState.moneyUsage)
                ImagesCard(uiState = uiState.moneyUsage)
                MailsSection(
                    linkedMails = uiState.linkedMails,
                )
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = "コピー",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            onClickDelete()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    text = "削除",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun MainCard(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState.MoneyUsage,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            MoneyUsageSection(
                title = "タイトル",
                content = { Text(uiState.title) },
                onClickChange = { uiState.event.onClickTitleChange() },
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
            MoneyUsageSection(
                title = "日付",
                content = { Text(uiState.date) },
                onClickChange = { uiState.event.onClickDateChange() },
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
            MoneyUsageSection(
                title = "時間",
                content = { Text(uiState.time) },
                onClickChange = { uiState.event.onClickTimeChange() },
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
            MoneyUsageSection(
                title = "カテゴリ",
                content = { Text(uiState.category) },
                onClickChange = { uiState.event.onClickCategoryChange() },
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
            MoneyUsageSection(
                title = "金額",
                content = { Text(uiState.amount) },
                onClickChange = { uiState.event.onClickAmountChange() },
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
            MoneyUsageSection(
                title = "説明",
                multiline = true,
                content = {
                    UrlClickableText(
                        text = uiState.description.text,
                        onClickUrl = { uiState.description.event.onClickUrl(it) },
                        onLongClickUrl = { uiState.description.event.onLongClickUrl(it) },
                    )
                },
                onClickChange = { uiState.event.onClickDescription() },
            )
        }
    }
}

@Composable
private fun MoneyUsageSection(
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    title: String,
    onClickChange: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                content()
            }
        }
        OutlinedButton(
            onClick = onClickChange,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                brush = SolidColor(MaterialTheme.colorScheme.primary),
            ),
        ) {
            Text("変更", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ImagesCard(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState.MoneyUsage,
) {
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "画像",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val allItems = remember(uiState.images, uiState.uploadQueueItems) {
                uiState.images.map { it to "image" } + uiState.uploadQueueItems.map { it to "queue" }
            }

            if (allItems.isNotEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val minItemWidth = 120.dp
                    val columnCount = (maxWidth / minItemWidth).toInt().coerceAtLeast(2)
                    val chunkedItems = allItems.chunked(columnCount)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunkedItems.forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { itemPair ->
                                    Box(
                                        modifier = Modifier.weight(1f).aspectRatio(1f),
                                    ) {
                                        val item = itemPair.first
                                        if (itemPair.second == "image") {
                                            val imageItem = item as MoneyUsageScreenUiState.ImageItem
                                            ImageItemContent(
                                                imageItem = imageItem,
                                                onClick = { selectedImageUrl = imageItem.url },
                                            )
                                        } else {
                                            val queueItem = item as MoneyUsageScreenUiState.UploadQueueItem
                                            UploadQueueItemContent(queueItem = queueItem)
                                        }
                                    }
                                }
                                // 空のセルを埋める
                                repeat(columnCount - rowItems.size) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_image),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "画像なし",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            ImageUploadButton(
                onClick = { uiState.event.onClickUploadImage() },
            )
        }
    }

    selectedImageUrl?.let { imageUrl ->
        ZoomableImageDialog(
            imageUrl = imageUrl,
            onDismissRequest = { selectedImageUrl = null },
        )
    }
}

@Composable
private fun ImageItemContent(
    imageItem: MoneyUsageScreenUiState.ImageItem,
    onClick: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPopupMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        SubcomposeAsyncImage(
            model = imageItem.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { showPopupMenu = true },
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val pressEvent = awaitPointerEvent()
                        if (pressEvent.type != PointerEventType.Press) return@awaitEachGesture
                        if (pressEvent.buttons.isSecondaryPressed.not()) return@awaitEachGesture

                        while (true) {
                            val releaseEvent = awaitPointerEvent()
                            if (releaseEvent.type != PointerEventType.Release) continue
                            showPopupMenu = true
                            break
                        }
                    }
                },
            loading = { ImageLoadingPlaceholder() },
        )
        DropdownMenu(
            expanded = showPopupMenu,
            onDismissRequest = { showPopupMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("削除") },
                onClick = {
                    showPopupMenu = false
                    showDeleteDialog = true
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
                    showDeleteDialog = false
                    imageItem.event.onClickDelete()
                },
                onClickNegative = { showDeleteDialog = false },
                onDismissRequest = { showDeleteDialog = false },
            )
        }
    }
}

@Composable
private fun UploadQueueItemContent(
    queueItem: MoneyUsageScreenUiState.UploadQueueItem,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(queueItem.id) {
                detectTapGestures(
                    onLongPress = { queueItem.onLongPressCancel() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (queueItem.previewBytes != null) {
            AsyncImage(
                model = queueItem.previewBytes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        if (queueItem.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (queueItem.isFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "失敗",
                        color = Color.White,
                    )
                    OutlinedButton(
                        onClick = { queueItem.onClickRetry() },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = "再試行",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MailsSection(
    modifier: Modifier = Modifier,
    linkedMails: ImmutableList<MoneyUsageScreenUiState.MailItem>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "連携されたメール",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
        if (linkedMails.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                linkedMails.forEach { mail ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { mail.event.onClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_email),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = mail.subject,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mail.from,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = mail.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("連携されたメールがありません")
                }
            }
        }
    }
}
