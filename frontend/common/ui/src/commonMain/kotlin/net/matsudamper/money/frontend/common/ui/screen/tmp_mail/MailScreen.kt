package net.matsudamper.money.frontend.common.ui.screen.tmp_mail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import MailScreenUiState
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.layout.ScrollButton
import net.matsudamper.money.frontend.common.ui.layout.html.html.Html
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MailImportScreen(
    uiState: MailScreenUiState,
    listener: RootScreenScaffoldListener,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    val html = uiState.htmlDialog
    if (html != null) {
        Html(
            html = html,
            onDismissRequest = {
                uiState.event.htmlDismissRequest()
            },
        )
    }
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        }
    } else {
        Scaffold(
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                KakeBoTopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = {
                        Text(
                            text = "メール インポート",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
            },
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
                    .padding(it)
            ) {
                val height = this.maxHeight
                val lazyListState = rememberLazyListState()
                val density = LocalDensity.current
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                ) {
                    items(uiState.mails) { item ->
                        Card(
                            modifier = Modifier.padding(32.dp),
                            onClick = {
                                item.onClick()
                            },
                        ) {
                            Text(
                                modifier = Modifier.padding(32.dp),
                                text = item.subject,
                                fontFamily = rememberCustomFontFamily(),
                            )
                        }
                    }
                }

                ScrollButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .padding(12.dp),
                    scrollState = lazyListState,
                    scrollSize = with(density) {
                        height.toPx() * 0.7f
                    },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
        }
    }
}
