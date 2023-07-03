package net.matsudamper.money.frontend.common.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import RootHomeScreenUiState
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.layout.html.html.Html


@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RootScreen(
    uiState: RootHomeScreenUiState,
    listener: RootScreenScaffoldListener,
) {
    val html = uiState.html
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
        RootScreenScaffold(
            modifier = Modifier.fillMaxSize(),
            currentScreen = Screen.Root.Home,
            listener = listener,
            content = {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
            },
        )
    }
}
