package net.matsudamper.money.frontend.feature.notification.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.AppSettingsRepository
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.feature.notification.ui.NotificationUsageFilterListScreenUiState

public class NotificationUsageFilterListViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val appSettingsRepository: AppSettingsRepository,
    parsers: List<NotificationUsageParser>,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val filterDefinitions = parsers.map { it.filterDefinition }

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<NotificationUsageFilterListScreenUiState> = MutableStateFlow(
        NotificationUsageFilterListScreenUiState(
            title = "通知フィルター",
            description = "フィルターごとに自動追加を切り替えます。ON にすると、その条件に一致した通知を受信時に自動追加します。未設定の項目は package 名 / 通知全文 / 0円 / 通知時刻で補完します。",
            filters = listOf<NotificationUsageFilterListScreenUiState.FilterItem>().toImmutableList(),
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        filters = viewModelState.enabledMap.entries.mapNotNull { (id, enabled) ->
                            val definition = filterDefinitions.find { it.id == id } ?: return@mapNotNull null
                            createFilterItem(
                                definition = definition,
                                autoAddEnabled = enabled,
                            )
                        }.toImmutableList(),
                    )
                }
            }
        }
        viewModelScope.launch {
            if (filterDefinitions.isEmpty()) return@launch

            combine(
                filterDefinitions.map { definition ->
                    appSettingsRepository.notificationUsageAutoAddEnabled(definition.id)
                        .map { definition.id to it }
                },
            ) { values ->
                values.toMap()
            }.collectLatest { enabledMap ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(enabledMap = enabledMap)
                }
            }
        }
    }.asStateFlow()

    private fun createFilterItem(
        definition: NotificationUsageFilterDefinition,
        autoAddEnabled: Boolean,
    ): NotificationUsageFilterListScreenUiState.FilterItem {
        return NotificationUsageFilterListScreenUiState.FilterItem(
            title = definition.title,
            description = definition.description,
            autoAddEnabled = autoAddEnabled,
            listener = object : NotificationUsageFilterListScreenUiState.FilterItemListener {
                override fun onToggleAutoAdd(value: Boolean) {
                    appSettingsRepository.setNotificationUsageAutoAddEnabled(definition.id, value)
                }
            },
        )
    }

    private data class ViewModelState(
        val enabledMap: Map<String, Boolean> = mapOf(),
    )
}
