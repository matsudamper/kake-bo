package net.matsudamper.money.frontend.common.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.root.RootSettingScreenUiState


@Composable
public fun RootSettingScreen(
    modifier: Modifier = Modifier,
    uiState: RootSettingScreenUiState,
    listener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Settings,
        listener = listener,
        content = {
            MainContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
            )
        },
    )
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    uiState: RootSettingScreenUiState,
) {
    Column(
        modifier = modifier,
    ) {
        val settingPaddingModifier = Modifier.padding(horizontal = 24.dp)
        Column(
            modifier = Modifier.then(settingPaddingModifier),
        ) {
            Text(
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 24.dp,
                ),
                text = "設定",
                fontFamily = rememberCustomFontFamily(),
            )
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            Modifier
                .then(settingPaddingModifier)
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 700.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            uiState.event.onClickImapButton()
                        },
                ) {
                    Text("IMAP")
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            uiState.event.onClickCategoryButton()
                        },
                ) {
                    Text("カテゴリ編集")
                }
            }
        }
    }
}

// TODO move to common
@Composable
internal fun SettingSection(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            title()
        }
        Spacer(Modifier.height(8.dp))
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            content()
        }
    }
}

// TODO move to common
@Composable
internal fun ChangeTextSection(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClickChange: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                title()
            }
            Spacer(Modifier.height(2.dp))
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                ),
            ) {
                text()
            }
        }
        OutlinedButton(
            onClick = { onClickChange() },
        ) {
            Text(
                text = "変更",
                fontFamily = rememberCustomFontFamily(),
            )
        }
    }
}
