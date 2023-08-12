package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
        public object None : Header
        public data class Calendar(
            val year: Int,
            val month: Int,
            val event: HeaderCalendarEvent
        ) : Header

        public data class List(
            val year: Int,
            val month: Int,
        ) : Header
    }

    public enum class Type {
        Calendar,
        List
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
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            listener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                menu = {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        OutlinedButton(
                            modifier = Modifier
                                .semantics(true) {
                                    contentDescription = "表示タイプ変更"
                                }
                                .align(Alignment.CenterEnd),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { expanded = !expanded },
                        ) {
                            when (uiState.type) {
                                RootUsageHostScreenUiState.Type.Calendar -> {
                                    Text(text = "カレンダー")
                                }

                                RootUsageHostScreenUiState.Type.List -> {
                                    Text(text = "リスト")
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    uiState.event.onClickCalendar()
                                },
                                text = {
                                    Text(text = "カレンダー")
                                },
                            )
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    uiState.event.onClickList()
                                },
                                text = {
                                    Text(text = "リスト")
                                },
                            )
                        }
                    }
                }
            )
        },
        content = {
            content()
        },
    )
}
