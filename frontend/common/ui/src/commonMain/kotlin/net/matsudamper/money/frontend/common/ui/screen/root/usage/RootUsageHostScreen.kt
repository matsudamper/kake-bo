package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.LocalIsLargeScreen
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput

public data class RootUsageHostScreenUiState(
    val type: Type,
    val header: Header,
    val textInputUiState: TextInputUiState?,
    val searchText: String,
    val event: Event,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public data class TextInputUiState(
        val title: String,
        val default: String,
        val inputType: TextFieldType,
        val textComplete: (String) -> Unit,
        val canceled: () -> Unit,
        val isMultiline: Boolean,
        val name: String,
    )

    public sealed interface Header {
        public data object None : Header

        public data class Calendar(
            val title: String,
            val year: Int,
            val month: Int,
            val event: HeaderCalendarEvent,
        ) : Header

        public data class List(
            val year: Int,
            val month: Int,
        ) : Header
    }

    public enum class Type {
        Calendar,
        List,
    }

    @Immutable
    public interface HeaderCalendarEvent {
        public fun onClickPrevMonth()

        public fun onClickNextMonth()

        public fun onClickYearMonth(year: Int, month: Int)
    }

    @Immutable
    public interface Event {
        public suspend fun onViewInitialized()

        public fun onClickCalendar()

        public fun onClickList()

        public fun onClickSearchBox()

        public fun onClickSearchBoxClear()

        public fun onClickAdd()
    }
}

@Composable
public fun RootUsageHostScreen(
    modifier: Modifier = Modifier,
    uiState: RootUsageHostScreenUiState,
    windowInsets: PaddingValues,
    content: @Composable () -> Unit,
) {
    if (uiState.textInputUiState != null) {
        FullScreenTextInput(
            title = uiState.textInputUiState.title,
            default = uiState.textInputUiState.default,
            inputType = uiState.textInputUiState.inputType,
            onComplete = uiState.textInputUiState.textComplete,
            canceled = uiState.textInputUiState.canceled,
            isMultiline = uiState.textInputUiState.isMultiline,
            name = uiState.textInputUiState.name,
        )
    }

    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }

    var isDatePickerExpanded by remember { mutableStateOf(false) }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                menu = {
                    Menu(
                        type = uiState.type,
                        onClickCalendar = uiState.event::onClickCalendar,
                        onClickList = uiState.event::onClickList,
                    )
                },
                title = {
                    TitleBar(
                        header = uiState.header,
                        isDatePickerExpanded = isDatePickerExpanded,
                        onClickDatePickerToggle = { isDatePickerExpanded = !isDatePickerExpanded },
                        onClickTitle = uiState.event::onClickCalendar,
                    )
                },
                windowInsets = windowInsets,
            )
        },
        content = {
            Column {
                val header = uiState.header
                if (header is RootUsageHostScreenUiState.Header.Calendar) {
                    AnimatedVisibility(
                        visible = isDatePickerExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        CalendarDatePicker(
                            selectedYear = header.year,
                            selectedMonth = header.month,
                            onSelectYearMonth = header.event::onClickYearMonth,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f),
                    ) {
                        SearchBox(
                            modifier = Modifier
                                .widthIn(max = 600.dp),
                            text = uiState.searchText,
                            onClick = { uiState.event.onClickSearchBox() },
                            onClickClear = { uiState.event.onClickSearchBoxClear() },
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        modifier = Modifier,
                        onClick = { uiState.event.onClickAdd() },
                        elevation = FloatingActionButtonDefaults.elevation(
                            0.dp,
                            0.dp,
                            0.dp,
                            0.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "add money usage",
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        },
    )
}

@Composable
private fun SearchBox(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClickClear: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "search",
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = text.ifEmpty { "検索" },
                )
            }
            if (text.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onClickClear() }
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "clear",
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBar(
    header: RootUsageHostScreenUiState.Header,
    isDatePickerExpanded: Boolean,
    onClickDatePickerToggle: () -> Unit,
    onClickTitle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hasControl = when (header) {
            is RootUsageHostScreenUiState.Header.Calendar -> false
            is RootUsageHostScreenUiState.Header.List -> true
            is RootUsageHostScreenUiState.Header.None -> false
        }
        if (hasControl && LocalIsLargeScreen.current.not()) {
            Text(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onClickTitle()
                },
                text = "家計簿",
            )
        }
        Spacer(modifier = Modifier.widthIn(8.dp))
        when (header) {
            is RootUsageHostScreenUiState.Header.Calendar -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.clip(CircleShape)
                            .clickable { header.event.onClickPrevMonth() }
                            .padding(8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前の月")
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { header.event.onClickNextMonth() }
                            .padding(8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "次の月")
                    }
                    Text(text = header.title)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onClickDatePickerToggle() }
                            .padding(8.dp),
                    ) {
                        Text(
                            text = if (isDatePickerExpanded) "▲" else "▼",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            is RootUsageHostScreenUiState.Header.List -> {
            }

            is RootUsageHostScreenUiState.Header.None -> Unit
        }
    }
}

@Composable
private fun CalendarDatePicker(
    selectedYear: Int,
    selectedMonth: Int,
    onSelectYearMonth: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val years = remember(selectedYear) { (selectedYear - 10..selectedYear + 10).toList() }
    val yearIndex = years.indexOf(selectedYear).takeIf { it >= 0 } ?: 10
    val yearListState = rememberLazyListState(
        initialFirstVisibleItemIndex = maxOf(0, yearIndex - 2),
    )
    val months = remember { (1..12).toList() }
    val monthIndex = (selectedMonth - 1).coerceIn(0, 11)
    val monthListState = rememberLazyListState(
        initialFirstVisibleItemIndex = maxOf(0, monthIndex - 2),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        ) {
            LazyRow(
                state = yearListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(years) { year ->
                    val isSelected = year == selectedYear
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectYearMonth(year, selectedMonth) },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "${year}年",
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                state = monthListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(months) { month ->
                    val isSelected = month == selectedMonth
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectYearMonth(selectedYear, month) },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "${month}月",
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Menu(
    type: RootUsageHostScreenUiState.Type,
    onClickCalendar: () -> Unit,
    onClickList: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(end = 8.dp)) {
        DropDownMenuButton(
            modifier = Modifier
                .semantics(true) {
                    contentDescription = "表示タイプ変更"
                }
                .align(Alignment.CenterEnd),
            onClick = { expanded = !expanded },
        ) {
            when (type) {
                RootUsageHostScreenUiState.Type.Calendar -> {
                    Text(text = "カレンダー")
                }

                RootUsageHostScreenUiState.Type.List -> {
                    Text(text = "リスト")
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    onClickCalendar()
                },
                text = {
                    Text(text = "カレンダー")
                },
            )
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    onClickList()
                },
                text = {
                    Text(text = "リスト")
                },
            )
        }
    }
}
