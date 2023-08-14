package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class RootHomeTabUiState(
    val contentType: ContentType,
    val event: Event,
) {
    public data class ColorText(
        val color: Color,
        val text: String,
        val onClick: () -> Unit,
    )

    public enum class ContentType {
        Between,
        Month,
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RootHomeTabScreen(
    uiState: RootHomeTabUiState,
    monthContent: @Composable () -> Unit,
    betweenContent: @Composable () -> Unit,
    scaffoldListener: RootScreenScaffoldListener,
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text("月別")
                        },
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Text("期間")
                        },
                    )
                }
                val holder = rememberSaveableStateHolder()
                when (uiState.contentType) {
                    RootHomeTabUiState.ContentType.Month -> {
                        holder.SaveableStateProvider(uiState.contentType) {
                            monthContent()
                        }
                    }

                    RootHomeTabUiState.ContentType.Between -> {
                        holder.SaveableStateProvider(uiState.contentType) {
                            betweenContent()
                        }
                    }
                }
            }
        },
    )
}
