package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.importedmail.html.ImportedMailHtmlScreen
import net.matsudamper.money.frontend.common.viewmodel.importedmail.html.ImportedMailHtmlViewModel
import net.matsudamper.money.ui.root.viewmodel.provideViewModel

@Composable
internal fun ImportedMailHtmlContainer(
    current: ScreenStructure.ImportedMailHTML,
    viewModelEventHandlers: ViewModelEventHandlers,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val viewModel = provideViewModel {
        ImportedMailHtmlViewModel(
            id = current.id,
            viewModelFeature = it,
            graphqlClient = koin.get(),
        )
    }
    LaunchedEffect(viewModel.viewModelEventHandler) {
        viewModelEventHandlers.handleImportedMailHtml(viewModel.viewModelEventHandler)
    }

    ImportedMailHtmlScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        kakeboScaffoldListener = kakeboScaffoldListener,
        windowInsets = windowInsets,
    )
}
