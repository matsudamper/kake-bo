package net.matsudamper.money.frontend.common.ui.screen.tmp_mail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.layout.html.html.Html

public data class MailLinkScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val fullScreenHtml: String?,
) {
    @Immutable
    public sealed interface LoadingState {
        public data class Loaded(
            val mails: ImmutableList<Mail>,
        ) : LoadingState

        public object Loading : LoadingState
    }

    public data class Mail(
        val mailFrom: String,
        val mailSubject: String,
        val title: String,
        val description: String,
        val price: String,
        val date: String,
        val event: MailEvent,
    )

    @Immutable
    public interface MailEvent {
        public fun onClickMailDetail()
        public fun onClickImportSuggestDetailButton()
    }

    @Immutable
    public interface Event {
        public fun onClickBackButton()
        public fun onViewInitialized()
        public fun dismissFullScreenHtml()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MailLinkScreen(
    modifier: Modifier = Modifier,
    uiState: MailLinkScreenUiState,
) {
    if (uiState.fullScreenHtml != null) {
        Html(
            html = uiState.fullScreenHtml,
            onDismissRequest = {
                uiState.event.dismissFullScreenHtml()
            },
        )
    }
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    Scaffold(
        modifier = modifier,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            KakeBoTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                navigationIcon = {
                    IconButton(onClick = { uiState.event.onClickBackButton() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "back",
                        )
                    }
                },
                title = {
                    Text(
                        text = "メールの登録",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
        },
        bottomBar = {
        },
    ) { paddingValues ->
        when (val loadingState = uiState.loadingState) {
            is MailLinkScreenUiState.LoadingState.Loaded -> {
                MainContent(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues),
                    uiState = loadingState,
                )
            }

            is MailLinkScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
public fun MainContent(
    modifier: Modifier,
    uiState: MailLinkScreenUiState.LoadingState.Loaded,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(uiState.mails) { mail ->
            ImportItem(
                modifier = Modifier.fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp,
                    ),
                mail = mail,
            )
        }
    }
}

@Composable
private fun ImportItem(
    modifier: Modifier = Modifier,
    mail: MailLinkScreenUiState.Mail,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp),
        ) {
            val textHorizontalPadding = 8.dp
            val textSpacerModifier = 4.dp

            MailItem(
                modifier = Modifier.fillMaxWidth(),
                textHorizontalPadding = textHorizontalPadding,
                from = mail.mailFrom,
                subject = mail.mailSubject,
                onClickDetail = mail.event::onClickMailDetail,
                textSpaceHeight = textSpacerModifier,
            )
            Spacer(Modifier.height(12.dp))
            ImportItem(
                modifier = Modifier.fillMaxWidth(),
                textHorizontalPadding = textHorizontalPadding,
                title = mail.title,
                description = mail.description,
                date = mail.date,
                price = mail.price,
                textSpaceHeight = textSpacerModifier,
                onClickDetail = { mail.event.onClickImportSuggestDetailButton() },
            )
        }
    }
}

@Composable
private fun ImportItem(
    modifier: Modifier = Modifier,
    textHorizontalPadding: Dp,
    title: String,
    description: String,
    date: String,
    price: String,
    onClickDetail: () -> Unit,
    textSpaceHeight: Dp,
) {
    val density = LocalDensity.current
    Column(modifier) {
        var titleMaxWidth by remember {
            mutableStateOf(0.dp)
        }
        val minSizeModifier = Modifier
            .widthIn(min = titleMaxWidth)
            .onSizeChanged {
                titleMaxWidth = max(titleMaxWidth, with(density) { it.width.toDp() })
            }
        Text(
            modifier = Modifier.padding(horizontal = textHorizontalPadding),
            text = "解析結果",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = rememberCustomFontFamily(),
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(1.dp),
            color = Color.White,
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(IntrinsicSize.Max),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MailItemCell(
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                    title = {
                        Text(
                            modifier = Modifier.then(minSizeModifier),
                            text = "タイトル",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                    description = {
                        Text(
                            modifier = Modifier.widthIn(min = titleMaxWidth),
                            text = title,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
                Spacer(Modifier.height(textSpaceHeight))
                MailItemCell(
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                    title = {
                        Text(
                            modifier = Modifier.then(minSizeModifier),
                            text = "日時",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                    description = {
                        Text(
                            text = date,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
                Spacer(Modifier.height(textSpaceHeight))
                MailItemCell(
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                    title = {
                        Text(
                            modifier = Modifier.then(minSizeModifier),
                            text = "説明",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                    description = {
                        Text(
                            text = description,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
                Spacer(Modifier.height(textSpaceHeight))
                MailItemCell(
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                    title = {
                        Text(
                            modifier = Modifier.then(minSizeModifier),
                            text = "金額",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                    description = {
                        Text(
                            text = price,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
            }
            Column(
                modifier = Modifier.fillMaxHeight(),
            ) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { onClickDetail() },
                ) {
                    Text(
                        text = "詳細",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MailItem(
    modifier: Modifier = Modifier,
    textHorizontalPadding: Dp,
    from: String,
    subject: String,
    onClickDetail: () -> Unit,
    textSpaceHeight: Dp,
) {
    val density = LocalDensity.current
    Column(
        modifier = modifier,
    ) {
        var titleMaxWidth by remember {
            mutableStateOf(0.dp)
        }
        val minSizeModifier = Modifier
            .widthIn(min = titleMaxWidth)
            .onSizeChanged {
                titleMaxWidth = max(titleMaxWidth, with(density) { it.width.toDp() })
            }
        Text(
            modifier = Modifier.padding(horizontal = textHorizontalPadding),
            text = "メール",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = rememberCustomFontFamily(),
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(1.dp),
            color = Color.White,
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(IntrinsicSize.Max),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MailItemCell(
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                    title = {
                        Text(
                            modifier = Modifier.then(minSizeModifier),
                            text = "From",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                    description = {
                        Text(
                            text = from,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
                Spacer(Modifier.height(textSpaceHeight))
                MailItemCell(
                    modifier = Modifier.padding(horizontal = textHorizontalPadding),
                    title = {
                        Text(
                            modifier = Modifier.then(minSizeModifier),
                            text = "タイトル",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                    description = {
                        Text(
                            text = subject,
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
                )
            }
            Column(
                modifier = Modifier.fillMaxHeight(),
            ) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { onClickDetail() },
                ) {
                    Text(
                        text = "詳細",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        }
    }
}

@Composable
public fun MailItemCell(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
) {
    Row(modifier = modifier) {
        title()
        Spacer(Modifier.width(12.dp))
        description()
    }
}