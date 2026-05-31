package net.matsudamper.money.frontend.common.viewmodel.root.settings.timezone

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.settings.TimezoneSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent

public class TimezoneSettingViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlApi: TimezoneSettingGraphqlApi,
    private val globalEventSender: EventSender<GlobalEvent>,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<TimezoneSettingScreenUiState> = MutableStateFlow(
        TimezoneSettingScreenUiState(
            loadingState = TimezoneSettingScreenUiState.LoadingState.Loading,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            event = object : TimezoneSettingScreenUiState.Event {
                override fun onClickBack() {
                    navController.back()
                }

                override fun onResume() {
                    load()
                }

                override fun onClickRetry() {
                    load()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { state ->
                uiStateFlow.update {
                    it.copy(
                        loadingState = toLoadingState(state),
                    )
                }
            }
        }
    }.asStateFlow()

    private fun formatOffsetText(offsetMinutes: Int): String {
        val sign = if (offsetMinutes >= 0) "+" else "-"
        val absMinutes = kotlin.math.abs(offsetMinutes)
        val hours = absMinutes / 60
        val minutes = absMinutes % 60
        val hoursStr = hours.toString().padStart(2, '0')
        return if (minutes == 0) {
            "UTC${sign}$hoursStr"
        } else {
            val minutesStr = minutes.toString().padStart(2, '0')
            "UTC${sign}$hoursStr:$minutesStr"
        }
    }

    private fun load() {
        viewModelScope.launch {
            if (viewModelStateFlow.value.timezoneOffsetMinutes == null) {
                viewModelStateFlow.update {
                    it.copy(loadingState = ViewModelState.LoadingState.Loading)
                }
            }

            val result = runCatching {
                graphqlApi.getTimezoneOffset()
            }.getOrNull()

            val offsetMinutes = result?.data?.user?.settings?.timezoneOffsetMinutes
            if (offsetMinutes == null) {
                viewModelStateFlow.update { state ->
                    if (state.timezoneOffsetMinutes == null) {
                        state.copy(loadingState = ViewModelState.LoadingState.Error)
                    } else {
                        state
                    }
                }
                return@launch
            }

            val (hours, minutes) = splitOffset(offsetMinutes)
            viewModelStateFlow.update {
                it.copy(
                    timezoneOffsetMinutes = offsetMinutes,
                    selectedHours = hours,
                    selectedMinutes = minutes,
                    loadingState = ViewModelState.LoadingState.Loaded,
                )
            }
        }
    }

    private fun applyTimezoneOffset(offsetMinutes: Int) {
        viewModelScope.launch {
            val result = runCatching {
                graphqlApi.setTimezoneOffset(offsetMinutes)
            }.onFailure {
                showNativeNotification("更新に失敗しました")
                return@launch
            }.getOrNull() ?: return@launch

            val updatedOffset = result.data
                ?.userMutation
                ?.settingsMutation
                ?.updateTimezoneOffset
            if (updatedOffset == null) {
                showNativeNotification("更新に失敗しました")
                return@launch
            }

            val (hours, minutes) = splitOffset(updatedOffset)
            viewModelStateFlow.update {
                it.copy(
                    timezoneOffsetMinutes = updatedOffset,
                    selectedHours = hours,
                    selectedMinutes = minutes,
                    loadingState = ViewModelState.LoadingState.Loaded,
                )
            }
        }
    }

    private fun splitOffset(offsetMinutes: Int): Pair<Int, Int> {
        val absMinutes = kotlin.math.abs(offsetMinutes)
        val hoursPart = absMinutes / 60
        val minutesPart = absMinutes % 60
        val roundedMinutes = listOf(0, 15, 30, 45).minByOrNull { kotlin.math.abs(it - minutesPart) } ?: 0
        return if (offsetMinutes >= 0) Pair(hoursPart, roundedMinutes) else Pair(-hoursPart, roundedMinutes)
    }

    private fun combineOffset(hours: Int, minutes: Int): Int {
        return if (hours >= 0) hours * 60 + minutes else hours * 60 - minutes
    }

    private fun getDeviceTimezoneOffsetMinutes(): Int {
        val tz = TimeZone.currentSystemDefault()
        val offset = tz.offsetAt(Clock.System.now())
        return offset.totalSeconds / 60
    }

    private fun toLoadingState(state: ViewModelState): TimezoneSettingScreenUiState.LoadingState {
        return when (state.loadingState) {
            ViewModelState.LoadingState.Error -> TimezoneSettingScreenUiState.LoadingState.Error
            ViewModelState.LoadingState.Loading -> TimezoneSettingScreenUiState.LoadingState.Loading
            ViewModelState.LoadingState.Loaded -> {
                val timezoneOffsetMinutes = state.timezoneOffsetMinutes
                    ?: return TimezoneSettingScreenUiState.LoadingState.Error
                TimezoneSettingScreenUiState.LoadingState.Loaded(
                    timezoneOffsetText = formatOffsetText(timezoneOffsetMinutes),
                    selectedHours = state.selectedHours,
                    selectedMinutes = state.selectedMinutes,
                    event = object : TimezoneSettingScreenUiState.LoadedEvent {
                        override fun onSelectHours(hours: Int) {
                            viewModelStateFlow.update {
                                it.copy(selectedHours = hours)
                            }
                        }

                        override fun onSelectMinutes(minutes: Int) {
                            viewModelStateFlow.update {
                                it.copy(selectedMinutes = minutes)
                            }
                        }

                        override fun onClickApply() {
                            applyTimezoneOffset(combineOffset(state.selectedHours, state.selectedMinutes))
                        }

                        override fun onClickSetDeviceTimezone() {
                            applyTimezoneOffset(getDeviceTimezoneOffsetMinutes())
                        }
                    },
                )
            }
        }
    }

    private suspend fun showNativeNotification(text: String) {
        globalEventSender.send {
            it.showNativeNotification(text)
        }
    }

    private data class ViewModelState(
        val timezoneOffsetMinutes: Int? = null,
        val selectedHours: Int = 0,
        val selectedMinutes: Int = 0,
        val loadingState: LoadingState = LoadingState.Loading,
    ) {
        enum class LoadingState {
            Loading,
            Loaded,
            Error,
        }
    }
}
