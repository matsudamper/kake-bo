package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.html.html.Html
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
public fun MailImportScreen(
    uiState: ImportMailScreenUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    val html = uiState.htmlDialog
    if (html != null) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
                .padding(windowInsets),
        ) {
            Html(
                html = html,
                onDismissRequest = {
                    uiState.event.htmlDismissRequest()
                },
            )
        }
    }

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
                            uiState.rootScreenScaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MailContent(
                uiState = uiState,
            )

            uiState.mailDeleteDialog?.let { mailDeleteDialog ->
                MailDeleteConfirmDialog(
                    uiState = mailDeleteDialog,
                )
            }
        }
    }
}

@Composable
private fun MailContent(uiState: ImportMailScreenUiState) {
    val density = LocalDensity.current
    val firstLoadingFinished = remember(uiState.isLoading, uiState.mails) {
        uiState.mails.isNotEmpty() || uiState.isLoading.not()
    }
    var containerHeight by remember { mutableIntStateOf(0) }
    val lazyListState = rememberLazyListState()
    Scaffold(
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            if (firstLoadingFinished) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            modifier = Modifier.padding(12.dp)
                                .widthIn(max = 500.dp)
                                .fillMaxWidth(),
                            onClick = { uiState.event.onClickImport() },
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp,
                            ),
                            shape = CircleShape,
                        ) {
                            Text(
                                text = "インポート",
                                fontSize = 18.sp,
                                fontFamily = rememberCustomFontFamily(),
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        if (firstLoadingFinished.not()) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                LaunchedEffect(maxHeight) {
                    containerHeight = with(density) {
                        maxHeight.roundToPx()
                    }
                }
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        state = lazyListState,
                        contentPadding = paddingValues,
                    ) {
                        items(uiState.mails) { item ->
                            MailContent(
                                modifier = Modifier
                                    .padding(vertical = 12.dp),
                                uiState = item,
                            )
                        }
                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        } else {
                            if (uiState.showLoadMore) {
                                item {
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { uiState.event.onClickLoadMore() },
                                    ) {
                                        Text(
                                            text = "もっと読み込む",
                                            fontFamily = rememberCustomFontFamily(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MailDeleteConfirmDialog(uiState: ImportMailScreenUiState.MailDeleteDialog) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                uiState.event.onDismiss()
            },
        contentAlignment = Alignment.Center,
    ) {
        Card {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(IntrinsicSize.Max)
                    .widthIn(max = 500.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "選択したメールを削除しますか？",
                    fontFamily = rememberCustomFontFamily(),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(24.dp))
                uiState.errorText?.let { errorText ->
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(24.dp))
                }
                Row(
                    modifier = Modifier.align(Alignment.End)
                        .height(intrinsicSize = IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }
                    TextButton(
                        onClick = { uiState.event.onClickCancel() },
                    ) {
                        Text(
                            text = "キャンセル",
                            maxLines = 1,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                    TextButton(
                        onClick = { uiState.event.onClickDelete() },
                    ) {
                        Text(
                            text = "削除",
                            maxLines = 1,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MailContent(
    modifier: Modifier = Modifier,
    uiState: ImportMailScreenUiState.Mail,
) {
    Card(
        modifier = modifier,
        onClick = { uiState.event.onClick() },
        colors = if (uiState.isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.1f),
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
                .height(IntrinsicSize.Min),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier,
                    text = "From: ${uiState.from}",
                    fontFamily = rememberCustomFontFamily(),
                    maxLines = 2,
                )
                Text(
                    modifier = Modifier,
                    text = "Sender: ${uiState.sender.orEmpty()}",
                    fontFamily = rememberCustomFontFamily(),
                    maxLines = 2,
                )
                Text(
                    modifier = Modifier,
                    text = "Subject: ${uiState.subject}",
                    fontFamily = rememberCustomFontFamily(),
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
            ) {
                Checkbox(
                    checked = uiState.isSelected,
                    onCheckedChange = null,
                )
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { uiState.event.onClickDelete() },
                ) {
                    Text(
                        modifier = Modifier,
                        text = "削除",
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
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
