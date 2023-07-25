package net.matsudamper.money.frontend.common.ui.screen.tmp_mail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar

public data class MailLinkScreenUiState(
    val event: Event,
    val mails: ImmutableList<Mail>,
) {
    public data class Mail(
        val from: String,
        val title: String,
        val price: Int?,
        val event: MailEvent,
    )

    @Immutable
    public interface MailEvent {
        public fun onClickDetail()
    }

    @Immutable
    public interface Event {
        public fun onClickBackButton()
        public fun onViewInitialized()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MailLinkScreen(
    modifier: Modifier = Modifier,
    uiState: MailLinkScreenUiState,
) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Text("TODO")
            }
            items(uiState.mails) { mail ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row {
                        Column {
                            Text(mail.from)
                            Text(mail.title)
                            Text("${mail.price}円")
                        }
                        Column {
                            OutlinedButton(onClick = { mail.event.onClickDetail() }) {
                                Text("詳細")
                            }
                        }
                    }
                }
            }
        }
    }
}