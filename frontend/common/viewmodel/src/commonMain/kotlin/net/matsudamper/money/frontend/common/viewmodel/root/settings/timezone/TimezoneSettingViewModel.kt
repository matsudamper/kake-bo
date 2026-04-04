package net.matsudamper.money.frontend.common.viewmodel.root.settings.timezone

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val ioDispatchers: CoroutineDispatcher,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<TimezoneSettingScreenUiState> = MutableStateFlow(
        TimezoneSettingScreenUiState(
            loadingState = TimezoneSettingScreenUiState.LoadingState.Loading,
            textInputEvent = null,
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

                override fun consumeTextInputEvent() {
                    viewModelStateFlow.update {
                        it.copy(textInputEvent = null)
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { state ->
                uiStateFlow.update {
                    it.copy(
                        loadingState = state.toLoadingState(),
                        textInputEvent = state.textInputEvent,
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
        val minutesStr = minutes.toString().padStart(2, '0')
        return "UTC${sign}$hoursStr:$minutesStr（${offsetMinutes}分）"
    }

    private fun load() {
        viewModelScope.launch {
            if (viewModelStateFlow.value.timezoneOffsetMinutes == null) {
                viewModelStateFlow.update {
                    it.copy(loadingState = ViewModelState.LoadingState.Loading)
                }
            }

            val result = runCatching {
                withContext(ioDispatchers) {
                    graphqlApi.getTimezoneOffset()
                }
            }.getOrNull()

            val offsetMinutes = result?.data?.user?.settings?.timezoneOffset
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

            viewModelStateFlow.update {
                it.copy(
                    timezoneOffsetMinutes = offsetMinutes,
                    loadingState = ViewModelState.LoadingState.Loaded,
                )
            }
        }
    }

    private val timezoneTextInputEvent = object : TimezoneSettingScreenUiState.TextInputUiState.Event {
        override fun complete(text: String) {
            val offsetMinutes = text.toIntOrNull()
            if (offsetMinutes == null) {
                viewModelScope.launch {
                    showNativeNotification("数値を入力してください")
                }
                return
            }
            if (offsetMinutes !in validTimezoneOffsetRange) {
                viewModelScope.launch {
                    showNativeNotification("UTC-12:00〜UTC+14:00 の範囲で入力してください")
                }
                return
            }
            viewModelScope.launch {
                val result = runCatching {
                    withContext(ioDispatchers) {
                        graphqlApi.setTimezoneOffset(offsetMinutes)
                    }
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

                viewModelStateFlow.update {
                    it.copy(
                        timezoneOffsetMinutes = updatedOffset,
                        textInputEvent = null,
                        loadingState = ViewModelState.LoadingState.Loaded,
                    )
                }
            }
        }

        override fun cancel() {
            viewModelStateFlow.update {
                it.copy(textInputEvent = null)
            }
        }
    }

    private fun ViewModelState.toLoadingState(): TimezoneSettingScreenUiState.LoadingState {
        return when (loadingState) {
            ViewModelState.LoadingState.Error -> TimezoneSettingScreenUiState.LoadingState.Error
            ViewModelState.LoadingState.Loading -> TimezoneSettingScreenUiState.LoadingState.Loading
            ViewModelState.LoadingState.Loaded -> {
                val timezoneOffsetMinutes = timezoneOffsetMinutes
                    ?: return TimezoneSettingScreenUiState.LoadingState.Error
                TimezoneSettingScreenUiState.LoadingState.Loaded(
                    timezoneOffsetMinutes = timezoneOffsetMinutes,
                    timezoneOffsetText = formatOffsetText(timezoneOffsetMinutes),
                    event = object : TimezoneSettingScreenUiState.LoadedEvent {
                        override fun onClickChange() {
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    textInputEvent = TimezoneSettingScreenUiState.TextInputUiState(
                                        title = "タイムゾーンオフセット（分）",
                                        default = viewModelState.timezoneOffsetMinutes?.toString().orEmpty(),
                                        event = timezoneTextInputEvent,
                                    ),
                                )
                            }
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
        val textInputEvent: TimezoneSettingScreenUiState.TextInputUiState? = null,
        val loadingState: LoadingState = LoadingState.Loading,
    ) {
        enum class LoadingState {
            Loading,
            Loaded,
            Error,
        }
    }

    private companion object {
        val validTimezoneOffsetRange: IntRange = -720..840
    }
}
