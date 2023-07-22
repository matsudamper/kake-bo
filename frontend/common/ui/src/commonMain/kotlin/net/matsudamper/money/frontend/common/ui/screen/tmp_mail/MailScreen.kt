package net.matsudamper.money.frontend.common.ui.screen.tmp_mail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MailImportScreen(
    uiState: MailScreenUiState,
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
                    .padding(it),
            ) {
                val height = this.maxHeight
                val lazyListState = rememberLazyListState()
                val density = LocalDensity.current
                Row(modifier = Modifier.fillMaxSize()) {
                    val listHorizontalPadding = 12.dp
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        state = lazyListState,
                    ) {
                        items(uiState.mails) { item ->
                            MailContent(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .padding(start = listHorizontalPadding),
                                uiState = item,
                            )
                        }
                    }

                    ScrollButton(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(listHorizontalPadding),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MailContent(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState.Mail,
) {
    Card(
        modifier = modifier,
        onClick = { uiState.event.onClick() },
        colors = if (uiState.isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.1f)
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        modifier = Modifier,
                        text = "From: ",
                        fontFamily = rememberCustomFontFamily(),
                    )
                    Text(
                        modifier = Modifier,
                        text = uiState.from,
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
                Row {
                    Text(
                        modifier = Modifier,
                        text = "Sender: ",
                        fontFamily = rememberCustomFontFamily(),
                    )
                    Text(
                        modifier = Modifier,
                        text = uiState.sender.orEmpty(),
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
                Row {
                    Text(
                        modifier = Modifier,
                        text = "Subject: ",
                        fontFamily = rememberCustomFontFamily(),
                    )
                    Text(
                        modifier = Modifier,
                        text = uiState.subject,
                        fontFamily = rememberCustomFontFamily(),
                        maxLines = 1,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Checkbox(
                    checked = uiState.isSelected,
                    onCheckedChange = null,
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { uiState.event.onClickDetail() },
                ) {
                    Text(
                        modifier = Modifier,
                        text = "詳細",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        }
    }
}
