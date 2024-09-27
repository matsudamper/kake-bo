package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.addmoneyusage.AddMoneyUsageScreen
import net.matsudamper.money.frontend.common.viewmodel.addmoneyusage.AddMoneyUsageScreenApi
import net.matsudamper.money.frontend.common.viewmodel.addmoneyusage.AddMoneyUsageViewModel
import net.matsudamper.money.ui.root.viewmodel.provideViewModel

@Composable
internal fun AddMoneyUsageScreenContainer(
    current: ScreenStructure.AddMoneyUsage,
    viewModelEventHandlers: ViewModelEventHandlers,
    rootCoroutineScope: CoroutineScope,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val viewModel = provideViewModel { viewModelFeature ->
        AddMoneyUsageViewModel(
            viewModelFeature = viewModelFeature,
            graphqlApi = AddMoneyUsageScreenApi(
                graphqlClient = koin.get(),
            ),
            graphqlClient = koin.get(),
        )
    }
    LaunchedEffect(viewModel.eventHandler) {
        viewModelEventHandlers.handleAddMoneyUsage(
            handler = viewModel.eventHandler,
        )
    }
    LaunchedEffect(current) {
        viewModel.updateScreenStructure(current)
    }
    AddMoneyUsageScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}
