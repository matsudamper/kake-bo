package net.matsudamper.money.frontend.common.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.layout.html.text.HtmlTextInput

@Composable
public fun RootSettingScreen(
    modifier: Modifier = Modifier,
    listener: RootScreenScaffoldListener,
) {
    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = Screen.Root.Settings,
        listener = listener,
        content = {
            Column(
                Modifier
                    .fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 32.dp,
                            vertical = 24.dp
                        ),
                        text = "設定",
                        fontFamily = rememberCustomFontFamily(),
                    )
                    Divider(modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    Modifier
                        .padding(horizontal = 32.dp)
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 700.dp),
                    ) {
                        SettingElementContent(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SettingElementContent(
    modifier: Modifier  = Modifier,
) {
    Column(modifier = modifier) {
        Column {
            Text("Host")
            Spacer(Modifier.height(8.dp))
            HtmlTextInput(
                modifier = Modifier.fillMaxWidth(),
                onValueChange = {},
                placeholder = "host",
                type = KeyboardType.Text
            )
        }
        Spacer(Modifier.height(12.dp))
        Column {
            Text("User Name")
            Spacer(Modifier.height(8.dp))
            HtmlTextInput(
                modifier = Modifier.fillMaxWidth(),
                onValueChange = {},
                placeholder = "user name",
                type = KeyboardType.Text
            )
        }
        Spacer(Modifier.height(12.dp))
        Column {
            Text("Port")
            Spacer(Modifier.height(8.dp))
            HtmlTextInput(
                modifier = Modifier.fillMaxWidth(),
                onValueChange = {},
                placeholder = "port",
                type = KeyboardType.Text
            )
        }
        Spacer(Modifier.height(12.dp))
        Column {
            Text("Password")
            Spacer(Modifier.height(8.dp))
            HtmlTextInput(
                modifier = Modifier.fillMaxWidth(),
                onValueChange = {},
                placeholder = "password",
                type = KeyboardType.Password
            )
        }
    }
}
