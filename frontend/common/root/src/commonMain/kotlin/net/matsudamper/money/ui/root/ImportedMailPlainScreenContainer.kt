package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.importedmail.plain.ImportedMailPlainScreen
import net.matsudamper.money.frontend.common.viewmodel.importedmail.plain.ImportedMailPlainViewModel
import net.matsudamper.money.ui.root.viewmodel.provideViewModel

@Composable
internal fun ImportedMailPlainScreenContainer(
    screen: ScreenStructure.ImportedMailPlain,
    viewModelEventHandlers: ViewModelEventHandlers,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val viewModel = provideViewModel {
        ImportedMailPlainViewModel(
            id = screen.id,
            viewModelFeature = it,
            graphqlClient = koin.get(),
        )
    }
    LaunchedEffect(viewModel.viewModelEventHandler) {
        viewModelEventHandlers.handleImportedMailPlain(viewModel.viewModelEventHandler)
    }

    ImportedMailPlainScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        kakeboScaffoldListener = kakeboScaffoldListener,
        windowInsets = windowInsets,
    )
}