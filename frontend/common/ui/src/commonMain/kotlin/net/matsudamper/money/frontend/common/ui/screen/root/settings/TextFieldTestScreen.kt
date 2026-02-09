package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold

public data class TextFieldTestScreenUiState(
    val kakeboScaffoldListener: KakeboScaffoldListener,
)

@Composable
public fun TextFieldTestScreen(
    modifier: Modifier = Modifier,
    uiState: TextFieldTestScreenUiState,
    windowInsets: PaddingValues,
) {
    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
        content = {
            SettingScaffold(
                modifier = Modifier.fillMaxSize(),
                title = {
                    Text(text = "テキストフィールドテスト")
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("TextField2 (BasicTextField)")
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        modifier = Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            .padding(12.dp),
                        state = rememberTextFieldState(),
                        textStyle = MaterialTheme.typography.bodyLarge.merge(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("TextField (Material3)")
                    Spacer(modifier = Modifier.height(8.dp))
                    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
                    TextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                        },
                    )
                }
            }
        },
    )
}
