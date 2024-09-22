package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class RootHomeTabScreenScaffoldUiState(
    val contentType: ContentType,
    val event: Event,
) {
    public enum class ContentType {
        Period,
        Monthly,
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()

        public fun onClickPeriod()

        public fun onClickMonth()
    }
}

@Composable
public fun RootHomeTabScreenScaffold(
    uiState: RootHomeTabScreenScaffoldUiState,
    scaffoldListener: RootScreenScaffoldListener,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit = {},
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Home,
        listener = scaffoldListener,
        topBar = {
            KakeBoTopAppBar(
                windowInsets=contentPadding,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier =
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                scaffoldListener.kakeboScaffoldListener.onClickTitle()
                            },
                            text = "家計簿",
                        )
                        menu()
                    }
                },
                menu = {
                    Menu(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentType = uiState.contentType,
                        onClickPeriod = uiState.event::onClickPeriod,
                        onClickMonth = uiState.event::onClickMonth,
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
private fun Menu(
    modifier: Modifier = Modifier,
    contentType: RootHomeTabScreenScaffoldUiState.ContentType,
    onClickPeriod: () -> Unit,
    onClickMonth: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        DropDownMenuButton(
            modifier =
            Modifier
                .semantics(true) {
                    contentDescription = "表示タイプ変更"
                }
                .align(Alignment.CenterEnd),
            onClick = { expanded = !expanded },
        ) {
            when (contentType) {
                RootHomeTabScreenScaffoldUiState.ContentType.Period -> {
                    Text(text = "期間")
                }

                RootHomeTabScreenScaffoldUiState.ContentType.Monthly -> {
                    Text(text = "月別")
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
                    onClickPeriod()
                },
                text = {
                    Text(text = "期間")
                },
            )
            DropdownMenuItem(
                onClick = {
                    expanded = false
                    onClickMonth()
                },
                text = {
                    Text(text = "月別")
                },
            )
        }
    }
}
