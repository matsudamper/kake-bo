package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class RootUsageHostScreenUiState(
    val type: Type,
    val header: Header,
    val event: Event,
) {
    public sealed interface Header {
        public data object None : Header
        public data class Calendar(
            val title: String,
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
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
        public fun onClickCalendar()
        public fun onClickList()
    }
}

@Composable
public fun RootUsageHostScreen(
    modifier: Modifier = Modifier,
    uiState: RootUsageHostScreenUiState,
    listener: RootScreenScaffoldListener,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.List,
        listener = listener,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    TitleBar(
                        header = uiState.header,
                        onClickTitle = uiState.event::onClickCalendar,
                    )
                },
                menu = {
                    Menu(
                        type = uiState.type,
                        onClickCalendar = uiState.event::onClickCalendar,
                        onClickList = uiState.event::onClickList,
                    )
                },
            )
        },
        content = {
            content()
        },
    )
}

@Composable
private fun TitleBar(
    header: RootUsageHostScreenUiState.Header,
    onClickTitle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onClickTitle()
            },
            text = "家計簿",
        )
        Spacer(modifier = Modifier.widthIn(12.dp))
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
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前の月")
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { header.event.onClickNextMonth() }
                            .padding(8.dp),
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "次の月")
                    }
                    Text(text = header.title)
                }
            }

            is RootUsageHostScreenUiState.Header.List -> {
            }

            is RootUsageHostScreenUiState.Header.None -> Unit
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
