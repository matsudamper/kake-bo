package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class HomeAddTabScreenUiState(
    val rootScreenScaffoldListener: RootScreenScaffoldListener,
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
    uiState: HomeAddTabScreenUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Add,
        listener = uiState.rootScreenScaffoldListener,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.rootScreenScaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
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
                        icon = {
                            Row {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                                Icon(Icons.Default.Email, contentDescription = null)
                            }
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
                        icon = {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Icon(Icons.Default.Email, contentDescription = null)
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
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .heightIn(min = 140.dp)
            .height(IntrinsicSize.Max)
            .fillMaxSize(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                icon()
            }
            Spacer(modifier = Modifier.weight(1f).fillMaxWidth())
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomStart,
            ) {
                title()
            }
        }
    }
}
