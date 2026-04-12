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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
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
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold

public data class HomeAddTabScreenUiState(
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val items: ImmutableList<Item>,
) {
    public data class Item(
        val title: String,
        val icon: Icon,
        val listener: ItemListener,
    )

    @Immutable
    public interface ItemListener {
        public fun onClick()
    }

    public enum class Icon {
        ImportMail,
        ImportedMail,
        Preset,
        Notification,
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
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.kakeboScaffoldListener.onClickTitle()
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
                items(
                    count = uiState.items.size,
                ) { index ->
                    val item = uiState.items[index]
                    Item(
                        title = {
                            Text(item.title)
                        },
                        icon = {
                            AddTabItemIcon(item.icon)
                        },
                        modifier = Modifier.padding(8.dp),
                        onClick = {
                            item.listener.onClick()
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

@Composable
private fun AddTabItemIcon(
    icon: HomeAddTabScreenUiState.Icon,
) {
    when (icon) {
        HomeAddTabScreenUiState.Icon.ImportMail -> {
            Row {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                Icon(Icons.Default.Email, contentDescription = null)
            }
        }

        HomeAddTabScreenUiState.Icon.ImportedMail -> {
            Row {
                Icon(Icons.Default.Add, contentDescription = null)
                Icon(Icons.Default.Email, contentDescription = null)
            }
        }

        HomeAddTabScreenUiState.Icon.Preset -> {
            Icon(Icons.Default.List, contentDescription = null)
        }

        HomeAddTabScreenUiState.Icon.Notification -> {
            Row {
                Icon(Icons.Default.Add, contentDescription = null)
                Icon(Icons.Default.Notifications, contentDescription = null)
            }
        }
    }
}
