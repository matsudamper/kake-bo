package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.importedmail.root.ImportedMailScreen
import net.matsudamper.money.frontend.common.viewmodel.importedmail.root.ImportedMailScreenGraphqlApi
import net.matsudamper.money.frontend.common.viewmodel.importedmail.root.ImportedMailScreenViewModel

@Composable
internal fun ImportedMailScreenContainer(
    current: ScreenStructure.ImportedMail,
    viewModelEventHandlers: ViewModelEventHandlers,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember(
        coroutineScope,
        current,
    ) {
        ImportedMailScreenViewModel(
            coroutineScope = coroutineScope,
            api = ImportedMailScreenGraphqlApi(
                graphqlClient = koin.get(),
            ),
            importedMailId = current.id,
        )
    }
    LaunchedEffect(viewModel.viewModelEventHandler) {
        viewModelEventHandlers.handleImportedMailScreen(
            handler = viewModel.viewModelEventHandler,
        )
    }

    ImportedMailScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}
