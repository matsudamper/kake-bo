package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.LocalCustomColors
import net.matsudamper.money.frontend.common.ui.lib.asWindowInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun KakeBoTopAppBar(
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    menu: @Composable () -> Unit = {},
    urlBar: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    windowInsets: PaddingValues,
) {
    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            windowInsets = windowInsets.asWindowInsets().only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            ),
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = navigation,
            title = {
                if (urlBar != null) {
                    val isFocused = LocalUrlBarFocused.current.value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedVisibility(
                            visible = !isFocused,
                            enter = expandHorizontally(expandFrom = Alignment.Start),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
                        ) {
                            title()
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            urlBar()
                        }
                        AnimatedVisibility(
                            visible = !isFocused,
                            enter = expandHorizontally(expandFrom = Alignment.End),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.End),
                        ) {
                            Box(contentAlignment = Alignment.CenterEnd) {
                                menu()
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        title()
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            menu()
                        }
                    }
                }
            },
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
            color = LocalCustomColors.current.menuDividerColor,
        )
    }
}
