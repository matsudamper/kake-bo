package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RootHomeTabScreenScaffold(
    uiState: RootHomeTabScreenScaffoldUiState,
    scaffoldListener: RootScreenScaffoldListener,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    RootScreenScaffold(
        modifier = Modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Home,
        listener = scaffoldListener,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            scaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(12.dp),
                ) {
                    FilterChip(
                        selected = uiState.contentType == RootHomeTabScreenScaffoldUiState.ContentType.Period,
                        onClick = { uiState.event.onClickPeriod() },
                        label = {
                            Text("期間")
                        },
                    )
                    Spacer(modifier = Modifier.widthIn(12.dp))
                    FilterChip(
                        selected = uiState.contentType == RootHomeTabScreenScaffoldUiState.ContentType.Monthly,
                        onClick = { uiState.event.onClickMonth() },
                        label = {
                            Text("月別")
                        },
                    )
                }
                content()
            }
        },
    )
}
