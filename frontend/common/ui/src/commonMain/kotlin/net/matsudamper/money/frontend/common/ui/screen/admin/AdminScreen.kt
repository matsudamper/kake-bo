package net.matsudamper.money.frontend.common.ui.screen.admin

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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_add
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_search
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_settings
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminRootScreen(
    modifier: Modifier = Modifier,
    uiState: AdminRootScreenUiState,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "管理画面",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                actions = {
                    TextButton(
                        onClick = uiState.listener::onClickLogout,
                    ) {
                        Text(
                            text = "ログアウト",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val horizontalPadding = ((maxWidth - 600.dp) / 2).coerceAtLeast(0.dp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    horizontal = horizontalPadding,
                    vertical = 8.dp,
                ),
            ) {
                item {
                    RootTileItem(
                        modifier = Modifier.padding(8.dp),
                        title = {
                            Text(
                                text = "ユーザー追加",
                                fontFamily = rememberCustomFontFamily(),
                            )
                        },
                        icon = {
                            Row {
                                Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = null)
                                Icon(painter = painterResource(Res.drawable.ic_settings), contentDescription = null)
                            }
                        },
                        onClick = { uiState.listener.onClickAddUser() },
                    )
                }
                item {
                    RootTileItem(
                        modifier = Modifier.padding(8.dp),
                        title = {
                            Text(
                                text = "未紐づき画像",
                                fontFamily = rememberCustomFontFamily(),
                            )
                        },
                        icon = {
                            Row {
                                Icon(painter = painterResource(Res.drawable.ic_settings), contentDescription = null)
                            }
                        },
                        onClick = { uiState.listener.onClickUnlinkedImages() },
                    )
                }
                item {
                    RootTileItem(
                        modifier = Modifier.padding(8.dp),
                        title = {
                            Text(
                                text = "ユーザー検索",
                                fontFamily = rememberCustomFontFamily(),
                            )
                        },
                        icon = {
                            Row {
                                Icon(painter = painterResource(Res.drawable.ic_search), contentDescription = null)
                            }
                        },
                        onClick = { uiState.listener.onClickUserSearch() },
                    )
                }
            }
        }
    }
}

@Composable
private fun RootTileItem(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
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
