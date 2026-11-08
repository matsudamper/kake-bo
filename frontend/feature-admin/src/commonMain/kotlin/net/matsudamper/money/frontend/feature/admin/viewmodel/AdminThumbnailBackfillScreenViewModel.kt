package net.matsudamper.money.frontend.feature.admin.viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.feature.admin.ui.AdminThumbnailBackfillScreenUiState
import net.matsudamper.money.frontend.graphql.AdminThumbnailBackfillSubscription
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.AdminThumbnailBackfillResult

private const val TAG = "AdminThumbnailBackfillScreenViewModel"

/**
 * 購読している間だけサーバーが処理する。停止ボタンか画面を離れると購読を切って止める
 */
internal class AdminThumbnailBackfillScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private var subscriptionJob: Job? = null

    private val event = object : AdminThumbnailBackfillScreenUiState.Event {
        override fun onClickStart() {
            start()
        }

        override fun onClickStop() {
            stop()
        }
    }

    public val uiStateFlow: StateFlow<AdminThumbnailBackfillScreenUiState> = MutableStateFlow(
        AdminThumbnailBackfillScreenUiState(
            lines = listOf(),
            isRunning = false,
            event = event,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        lines = viewModelState.lines,
                        isRunning = viewModelState.isRunning,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun start() {
        if (subscriptionJob?.isActive == true) return
        viewModelStateFlow.update { it.copy(isRunning = true) }
        appendLine("接続中...", AdminThumbnailBackfillScreenUiState.Level.INFO)

        subscriptionJob = viewModelScope.launch {
            var successCount = 0
            var failedCount = 0
            graphqlClient.apolloClient.subscription(AdminThumbnailBackfillSubscription())
                .toFlow()
                .onCompletion { cause ->
                    val summary = "成功 $successCount 件、失敗 $failedCount 件"
                    when (cause) {
                        null -> appendLine("完了: $summary", AdminThumbnailBackfillScreenUiState.Level.INFO)
                        is kotlinx.coroutines.CancellationException -> appendLine("停止: $summary", AdminThumbnailBackfillScreenUiState.Level.INFO)
                        else -> appendLine("切断: $summary", AdminThumbnailBackfillScreenUiState.Level.ERROR)
                    }
                    viewModelStateFlow.update { it.copy(isRunning = false) }
                }
                .catch { e ->
                    Logger.e(TAG, e)
                    appendLine("エラー: ${e.message}", AdminThumbnailBackfillScreenUiState.Level.ERROR)
                }
                .collect { response ->
                    val event = response.data?.adminThumbnailBackfill
                    if (event == null) {
                        val message = response.exception?.message
                            ?: response.errors?.joinToString { it.message }
                            ?: "unknown"
                        appendLine("エラー: $message", AdminThumbnailBackfillScreenUiState.Level.ERROR)
                        return@collect
                    }
                    when (event.result) {
                        AdminThumbnailBackfillResult.SUCCESS -> {
                            successCount++
                            appendLine("imageId=${event.imageId.value} ok", AdminThumbnailBackfillScreenUiState.Level.SUCCESS)
                        }

                        AdminThumbnailBackfillResult.FAILED,
                        AdminThumbnailBackfillResult.UNKNOWN__,
                        -> {
                            failedCount++
                            appendLine("imageId=${event.imageId.value} failed: ${event.message}", AdminThumbnailBackfillScreenUiState.Level.ERROR)
                        }
                    }
                }
        }
    }

    private fun stop() {
        subscriptionJob?.cancel()
        subscriptionJob = null
    }

    private fun appendLine(
        text: String,
        level: AdminThumbnailBackfillScreenUiState.Level,
    ) {
        viewModelStateFlow.update {
            it.copy(lines = it.lines + AdminThumbnailBackfillScreenUiState.Line(text = text, level = level))
        }
    }

    private data class ViewModelState(
        val lines: List<AdminThumbnailBackfillScreenUiState.Line> = listOf(),
        val isRunning: Boolean = false,
    )
}
