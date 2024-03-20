package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class HomeAddTabScreenUiState(
    val event: Event,
) {
    @Immutable
    public interface Event {
        public fun onClickImportButton()
        public fun onClickImportedButton()
    }
}


@Composable
public fun HomeAddTabScreen(
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    uiState: HomeAddTabScreenUiState,
    modifier: Modifier = Modifier,
) {
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Add,
        listener = rootScreenScaffoldListener,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier =
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            rootScreenScaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
            )
        },
    ) {
        BoxWithConstraints {
            val padding = ((maxWidth - 600.dp) / 2).coerceAtLeast(0.dp)
            LazyVerticalGrid(
                modifier = Modifier,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    horizontal = padding,
                ),
            ) {
                item {
                    Item(
                        title = {
                            Text("メールのインポート")
                        },
                        modifier = Modifier.padding(8.dp),
                        onClick = {
                            uiState.event.onClickImportButton()
                        },
                    )
                }
                item {
                    Item(
                        title = {
                            Text("インポートされたメールから追加")
                        },
                        modifier = Modifier.padding(8.dp),
                        onClick = {
                            uiState.event.onClickImportedButton()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Item(
    title: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(8.dp),
        ) {
            title()
        }
    }
}