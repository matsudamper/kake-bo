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
import net.matsudamper.money.frontend.common.ui.screen.moneyusage.MoneyUsageScreen
import net.matsudamper.money.frontend.common.viewmodel.moneyusage.MoneyUsageScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.moneyusage.MoneyUsageScreenViewModelApi
import net.matsudamper.money.ui.root.viewmodel.provideViewModel

@Composable
internal fun MoneyUsageContainer(
    screen: ScreenStructure.MoneyUsage,
    viewModelEventHandlers: ViewModelEventHandlers,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val viewModel = provideViewModel {
        MoneyUsageScreenViewModel(
            moneyUsageId = screen.id,
            viewModelFeature = it,
            api = MoneyUsageScreenViewModelApi(
                graphqlClient = koin.get(),
            ),
            graphqlClient = koin.get(),
        )
    }
    LaunchedEffect(viewModel.eventHandler) {
        viewModelEventHandlers.handleMoneyUsageScreen(viewModel.eventHandler)
    }
    MoneyUsageScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        kakeboScaffoldListener = kakeboScaffoldListener,
        windowInsets = windowInsets,
    )
}