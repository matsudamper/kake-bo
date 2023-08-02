package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.screen.root.ImportMailScreenUiState
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class HomeMailTabScreenUiState(
    val event: Event,
) {
    @Immutable
    public interface Event {
        public fun onClickImportTabButton()
        public fun onClickImportedTabButton()
    }
}

@Composable
public fun HomeMailTabScreen(
    modifier: Modifier = Modifier,
    uiState: HomeMailTabScreenUiState,
    screenStructure: ScreenStructure.Root.Mail,
    importMailScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail.Import) -> ImportMailScreenUiState,
    importedImportMailScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail.Imported) -> ImportedMailListScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    val holder = rememberSaveableStateHolder()

    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Mail,
        listener = rootScreenScaffoldListener,
    ) {
        Column(modifier = modifier) {
            TabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = when (screenStructure) {
                    is ScreenStructure.Root.Mail.Import -> 0
                    is ScreenStructure.Root.Mail.Imported -> 1
                },
            ) {
                Tab(
                    selected = true,
                    onClick = { uiState.event.onClickImportTabButton() },
                    text = {
                        Text(
                            text = "インポート",
                        )
                    },
                )
                Tab(
                    selected = false,
                    onClick = { uiState.event.onClickImportedTabButton() },
                    text = {
                        Text(
                            text = "インポート済み",
                        )
                    },
                )
            }
            when (screenStructure) {
                is ScreenStructure.Root.Mail.Import -> {
                    holder.SaveableStateProvider(screenStructure.toString()) {
                        MailImportScreen(
                            uiState = importMailScreenUiStateProvider(screenStructure),
                        )
                    }
                }

                is ScreenStructure.Root.Mail.Imported -> {
                    holder.SaveableStateProvider(screenStructure.toString()) {
                        ImportedMailListScreen(
                            uiState = importedImportMailScreenUiStateProvider(screenStructure),
                        )
                    }
                }
            }
        }
    }
}
