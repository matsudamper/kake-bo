package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class ImportedMailFilterCategoryScreenUiState(
    val loadingState: LoadingState,
    val textInput: TextInput? = null,
    val event: Event,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public object Error : LoadingState
        public data class Loaded(
            val title: String,
            val category: Category?,
        ) : LoadingState
    }

    public data class Category(
        val category: String,
        val subCategory: String,
    )

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
            when (val state = uiState.loadingState) {
                is ImportedMailFilterCategoryScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize(),
                        onClickRetry = { uiState.event.onViewInitialized() },
                    )
                }

                is ImportedMailFilterCategoryScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                is ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = state,
                    )
                }
            }
        }
    }
}


@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded,
) {
    Column {
        Text(uiState.title)
        Text(uiState.category.toString())
    }
}
