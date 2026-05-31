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

            applyLoadedOffset(offsetMinutes)
        }
    }

    private fun applyLoadedOffset(offsetMinutes: Int) {
        val isHourUnit = offsetMinutes % 60 == 0
        val unit = if (isHourUnit) {
            TimezoneSettingScreenUiState.OffsetUnit.Hour
        } else {
            TimezoneSettingScreenUiState.OffsetUnit.Minute
        }
        val inputValue = if (isHourUnit) {
            (offsetMinutes / 60).toString()
        } else {
            offsetMinutes.toString()
        }
        viewModelStateFlow.update {
            it.copy(
                timezoneOffsetMinutes = offsetMinutes,
                inputValue = inputValue,
                unit = unit,
                loadingState = ViewModelState.LoadingState.Loaded,
            )
        }
    }

    private fun applyTimezoneOffset(offsetMinutes: Int) {
        val validOffsetRange = -720..840
        if (offsetMinutes !in validOffsetRange) {
            viewModelScope.launch {
                showNativeNotification("UTC-12:00〜UTC+14:00 の範囲で入力してください")
            }
            return
        }
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

            applyLoadedOffset(updatedOffset)
        }
    }

    private fun convertValue(
        value: String,
        from: TimezoneSettingScreenUiState.OffsetUnit,
        to: TimezoneSettingScreenUiState.OffsetUnit,
    ): String {
        val intValue = value.toIntOrNull() ?: return value
        return when {
            from == to -> value
            // 時間 -> 分
            to == TimezoneSettingScreenUiState.OffsetUnit.Minute -> (intValue * 60).toString()
            // 分 -> 時間（切り捨て）
            else -> (intValue / 60).toString()
        }
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
                    inputValue = state.inputValue,
                    unit = state.unit,
                    event = object : TimezoneSettingScreenUiState.LoadedEvent {
                        override fun onChangeValue(value: String) {
                            viewModelStateFlow.update {
                                it.copy(inputValue = value)
                            }
                        }

                        override fun onSelectUnit(unit: TimezoneSettingScreenUiState.OffsetUnit) {
                            if (unit == state.unit) {
                                return
                            }
                            val convertedValue = convertValue(state.inputValue, state.unit, unit)
                            viewModelStateFlow.update {
                                it.copy(
                                    inputValue = convertedValue,
                                    unit = unit,
                                )
                            }
                        }

                        override fun onClickApply() {
                            val value = state.inputValue.toIntOrNull()
                            if (value == null) {
                                viewModelScope.launch {
                                    showNativeNotification("数値を入力してください")
                                }
                                return
                            }
                            val offsetMinutes = when (state.unit) {
                                TimezoneSettingScreenUiState.OffsetUnit.Hour -> value * 60
                                TimezoneSettingScreenUiState.OffsetUnit.Minute -> value
                            }
                            applyTimezoneOffset(offsetMinutes)
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
        val inputValue: String = "",
        val unit: TimezoneSettingScreenUiState.OffsetUnit = TimezoneSettingScreenUiState.OffsetUnit.Hour,
        val loadingState: LoadingState = LoadingState.Loading,
    ) {
        enum class LoadingState {
            Loading,
            Loaded,
            Error,
        }
    }
}
