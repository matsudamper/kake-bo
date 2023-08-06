package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class ImportedMailFilterCategoryScreenUiState(
    val textInput: TextInput? = null,
    val event: Event,
) {
    public data class TextInput(
        val title: String,
        val onCompleted: (String) -> Unit,
        val dismiss: () -> Unit,
    )

    @Immutable
    public interface Event {
        public fun onViewInitialized()
    }
}
@Composable
public fun ImportedMailFilterCategoryScreen(
    modifier: Modifier = Modifier,
    uiState: ImportedMailFilterCategoryScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    uiState.textInput?.also { textInput ->
        HtmlFullScreenTextInput(
            title = textInput.title,
            onComplete = { textInput.onCompleted(it) },
            canceled = { textInput.dismiss() },
            default = "",
            isMultiline = false,
        )
    }
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Settings,
        listener = rootScreenScaffoldListener,
    ) {
        SettingScaffold(
            title = {
                Text("メールカテゴリフィルタ")
            },
        ) {

        }
    }
}
