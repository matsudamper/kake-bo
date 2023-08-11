package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class RootUsageHostScreenUiState(
    val type: Type,
    val event: Event,
) {
    public enum class Type {
        Calendar,
        List
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
        menu = {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    when (uiState.type) {
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
                        onClick = { uiState.event.onClickCalendar() },
                        text = {
                            Text(text = "カレンダー")
                        },
                    )
                    DropdownMenuItem(
                        onClick = { uiState.event.onClickList() },
                        text = {
                            Text(text = "リスト")
                        },
                    )
                }
            }
        },
        content = {
            content()
        },
    )
}
