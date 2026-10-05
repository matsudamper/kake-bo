package net.matsudamper.money.frontend.common.viewmodel.importedmail.root

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.importedmail.root.MailScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EqualsImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery
import net.matsudamper.money.frontend.graphql.fragment.ImportedMailScreenSuggestUsage
import net.matsudamper.money.frontend.graphql.type.ImportedMailAiParseStatus

private const val TAG = "ImportedMailScreenViewModel"
private const val AI_PARSE_POLLING_INTERVAL_MILLIS = 3000L

public class ImportedMailScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val api: ImportedMailScreenGraphqlApi,
    private val importedMailId: ImportedMailId,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()
    private val event = object : MailScreenUiState.Event {
        override fun onClickRetry() {
            fetch()
        }

        override fun onClickArrowBackButton() {
            viewModelScope.launch {
                viewModelEventSender.send {
                    it.navigateToBack()
                }
            }
        }

        override fun onClickTitle() {
            viewModelScope.launch {
                viewModelEventSender.send {
                    it.navigateToHome()
                }
            }
        }

        override fun onClickDelete() {
            viewModelScope.launch {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        confirmDialog = MailScreenUiState.AlertDialog(
                            onDismissRequest = { dismissConfirmDialog() },
                            onClickNegative = { dismissConfirmDialog() },
                            onClickPositive = {
                                viewModelScope.launch {
                                    val isSuccess = api.delete(id = importedMailId)
                                    if (isSuccess) {
                                        dismissConfirmDialog()
                                        viewModelEventSender.send { it.navigateToBack() }
                                    }
                                }
                            },
                            title = "削除しますか？",
                        ),
                    )
                }
            }
        }

        override fun onResume() {
            fetch()
        }
    }

    public val uiStateFlow: StateFlow<MailScreenUiState> = MutableStateFlow(
        MailScreenUiState(
            loadingState = MailScreenUiState.LoadingState.Loading,
            event = event,
            confirmDialog = null,
            urlMenuDialog = null,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        confirmDialog = viewModelState.confirmDialog,
                        urlMenuDialog = viewModelState.urlMenuDialog,
                        loadingState = run {
                            val apolloResult = viewModelState.apolloResponse
                            if (apolloResult == null) {
                                return@run MailScreenUiState.LoadingState.Loading
                            }

                            if (apolloResult.isFailure) {
                                Logger.e(TAG, apolloResult.exceptionOrNull() ?: Exception("unknown error"))
                                return@run MailScreenUiState.LoadingState.Error
                            }

                            val response = apolloResult.getOrThrow()
                            val mail = response.data?.user?.importedMailAttributes?.mail

                            if (response.hasErrors() || mail == null) {
                                return@run MailScreenUiState.LoadingState.Error
                            }

                            createLoadedUiState(
                                mail = mail,
                                aiParseStartErrorMessage = viewModelState.aiParseStartErrorMessage,
                            )
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createLoadedUiState(
        mail: ImportedMailScreenQuery.Mail,
        aiParseStartErrorMessage: String?,
    ): MailScreenUiState.LoadingState.Loaded {
        return MailScreenUiState.LoadingState.Loaded(
            mail = MailScreenUiState.Mail(
                title = mail.subject,
                date = Formatter.formatDateTime(mail.dateTime),
                from = mail.from,
            ),
            usage = mail.usages.map {
                MailScreenUiState.LinkedUsage(
                    title = it.title,
                    amount = run amount@{
                        val splitAmount = Formatter.formatMoney(it.amount)
                        "${splitAmount}円"
                    },
                    category = run category@{
                        val subCategory = it.moneyUsageSubCategory ?: return@category null
                        val category = subCategory.category

                        "${category.name} / ${subCategory.name}"
                    },
                    date = Formatter.formatDateTime(it.date),
                    event = object : MailScreenUiState.LinkedUsageEvent {
                        override fun onClick() {
                            viewModelScope.launch {
                                viewModelEventSender.send { event ->
                                    event.navigate(
                                        ScreenStructure.MoneyUsage(
                                            id = it.id,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                )
            }.toImmutableList(),
            usageSuggest = mail.suggestUsages.mapIndexed { index, suggestUsage ->
                createUsageSuggest(
                    suggestUsage = suggestUsage.importedMailScreenSuggestUsage,
                    index = index,
                    isAiParseResult = false,
                )
            }.toImmutableList(),
            aiParse = MailScreenUiState.AiParse(
                state = createAiParseState(mail.aiParseResult),
                startErrorMessage = aiParseStartErrorMessage,
            ),
            hasHtml = mail.hasHtml,
            hasPlain = mail.hasPlain,
            event = object : MailScreenUiState.LoadedEvent {
                override fun onClickMailHtml() {
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.navigate(
                                ScreenStructure.ImportedMailHTML(
                                    id = importedMailId,
                                ),
                            )
                        }
                    }
                }

                override fun onClickMailPlain() {
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.navigate(
                                ScreenStructure.ImportedMailPlain(
                                    id = importedMailId,
                                ),
                            )
                        }
                    }
                }

                override fun onClickRegister() {
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.navigate(
                                ScreenStructure.AddMoneyUsage(
                                    importedMailId = importedMailId,
                                ),
                            )
                        }
                    }
                }

                override fun onClickAiParse() {
                    startAiParse()
                }
            },
        )
    }

    private fun createAiParseState(aiParseResult: ImportedMailScreenQuery.AiParseResult?): MailScreenUiState.AiParseState {
        if (aiParseResult == null) return MailScreenUiState.AiParseState.NotExecuted
        return when (aiParseResult.status) {
            ImportedMailAiParseStatus.RUNNING -> MailScreenUiState.AiParseState.Running
            ImportedMailAiParseStatus.SUCCEEDED -> MailScreenUiState.AiParseState.Succeeded(
                usageSuggest = aiParseResult.usages.mapIndexed { index, suggestUsage ->
                    createUsageSuggest(
                        suggestUsage = suggestUsage.importedMailScreenSuggestUsage,
                        index = index,
                        isAiParseResult = true,
                    )
                }.toImmutableList(),
            )

            ImportedMailAiParseStatus.FAILED,
            ImportedMailAiParseStatus.UNKNOWN__,
            -> MailScreenUiState.AiParseState.Failed(
                message = aiParseResult.errorMessage.orEmpty(),
            )
        }
    }

    private fun createUsageSuggest(
        suggestUsage: ImportedMailScreenSuggestUsage,
        index: Int,
        isAiParseResult: Boolean,
    ): MailScreenUiState.UsageSuggest {
        return MailScreenUiState.UsageSuggest(
            title = suggestUsage.title,
            serviceName = suggestUsage.serviceName.orEmpty(),
            amount = run amount@{
                val amount = suggestUsage.amount ?: return@amount null

                val splitAmount = Formatter.formatMoney(amount)
                "${splitAmount}円"
            },
            category = run category@{
                val subCategory = suggestUsage.subCategory ?: return@category null
                val category = subCategory.category

                "${category.name} / ${subCategory.name}"
            },
            description = run {
                MailScreenUiState.Clickable(
                    text = suggestUsage.description,
                    event = ClickableEventImpl(suggestUsage.description),
                )
            },
            dateTime = run dateTime@{
                val dateTIme = suggestUsage.dateTime ?: return@dateTime ""
                Formatter.formatDateTime(dateTIme)
            },
            event = object : MailScreenUiState.UsageSuggest.Event {
                override fun onClickRegister() {
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.navigate(
                                ScreenStructure.AddMoneyUsage(
                                    importedMailId = importedMailId,
                                    importedMailIndex = index,
                                    isAiParseResult = isAiParseResult,
                                ),
                            )
                        }
                    }
                }
            },
        )
    }

    init {
        fetch()
        viewModelScope.launch {
            viewModelStateFlow
                .map { it.shouldPollAiParseResult() }
                .distinctUntilChanged()
                .collectLatest { shouldPoll ->
                    if (shouldPoll.not()) return@collectLatest
                    while (true) {
                        delay(AI_PARSE_POLLING_INTERVAL_MILLIS)
                        fetchAiParseResult()
                    }
                }
        }
    }

    private fun fetch() {
        viewModelScope.launch {
            fetchAndUpdate()
        }
    }

    /**
     * 一時的な取得失敗で実行中の判定が外れるとポーリングが止まるため、有効な応答だけを反映する
     */
    private suspend fun fetchAiParseResult() {
        val requestedGeneration = viewModelStateFlow.value.aiParseStartGeneration
        val result = api.get(id = importedMailId)
        if (result.isValidResponse().not()) return

        viewModelStateFlow.update { viewModelState ->
            if (viewModelState.aiParseStartGeneration != requestedGeneration) return@update viewModelState
            viewModelState.copy(
                apolloResponse = result,
                isAwaitingAiParseResult = false,
                // 開始結果が不明だった場合も、サーバーの状態を取得できたのでそちらの表示に任せる
                aiParseStartErrorMessage = if (viewModelState.isAwaitingAiParseResult) {
                    null
                } else {
                    viewModelState.aiParseStartErrorMessage
                },
            )
        }
    }

    private suspend fun fetchAndUpdate() {
        val requestedGeneration = viewModelStateFlow.value.aiParseStartGeneration
        val result = api.get(id = importedMailId)

        viewModelStateFlow.update { viewModelState ->
            if (viewModelState.aiParseStartGeneration != requestedGeneration) return@update viewModelState
            viewModelState.copy(
                apolloResponse = result,
                isAwaitingAiParseResult = if (result.isValidResponse()) {
                    false
                } else {
                    viewModelState.shouldPollAiParseResult()
                },
            )
        }
    }

    /**
     * メールが削除されている場合も mail = null の正常な応答になるため、mail の有無では判定しない
     */
    private fun Result<ApolloResponse<ImportedMailScreenQuery.Data>>.isValidResponse(): Boolean {
        val response = getOrNull() ?: return false
        if (response.hasErrors()) return false
        return response.data?.user?.importedMailAttributes != null
    }

    private fun startAiParse() {
        viewModelScope.launch {
            viewModelStateFlow.update {
                it.copy(
                    aiParseStartErrorMessage = null,
                    aiParseStartGeneration = it.aiParseStartGeneration + 1,
                )
            }
            when (api.startAiParse(id = importedMailId)) {
                ImportedMailScreenGraphqlApi.StartAiParseResult.Success,
                ImportedMailScreenGraphqlApi.StartAiParseResult.AlreadyRunning,
                -> {
                    viewModelStateFlow.update { it.copy(isAwaitingAiParseResult = true) }
                    fetchAiParseResult()
                }

                ImportedMailScreenGraphqlApi.StartAiParseResult.ApiKeyNotSet -> {
                    viewModelStateFlow.update { it.copy(aiParseStartErrorMessage = "Gemini APIキーが設定されていません。設定画面から登録してください") }
                }

                ImportedMailScreenGraphqlApi.StartAiParseResult.MailNotFound -> {
                    viewModelStateFlow.update { it.copy(aiParseStartErrorMessage = "メールが見つかりませんでした") }
                }

                ImportedMailScreenGraphqlApi.StartAiParseResult.Failure -> {
                    viewModelStateFlow.update { it.copy(aiParseStartErrorMessage = "解析を開始できませんでした") }
                }

                ImportedMailScreenGraphqlApi.StartAiParseResult.Unknown -> {
                    // 応答だけを受け取れずサーバー側では開始している場合があるため、状態を取得できるまで確認する
                    viewModelStateFlow.update {
                        it.copy(
                            aiParseStartErrorMessage = "通信に失敗しました。解析状態を確認しています",
                            isAwaitingAiParseResult = true,
                        )
                    }
                    fetchAiParseResult()
                }
            }
        }
    }

    private fun dismissConfirmDialog() {
        viewModelScope.launch {
            viewModelStateFlow.update {
                it.copy(
                    confirmDialog = null,
                )
            }
        }
    }

    private inner class ClickableEventImpl(
        private val text: String,
    ) : MailScreenUiState.ClickableEvent, EqualsImpl(text) {
        override fun onClickUrl(url: String) {
            val dialog = MailScreenUiState.UrlMenuDialog(
                url = url,
                event = object : MailScreenUiState.UrlMenuDialogEvent {
                    override fun onClickOpen() {
                        viewModelScope.launch {
                            viewModelEventSender.send {
                                it.openWeb(text)
                            }
                        }
                        dismiss()
                    }

                    override fun onClickCopy() {
                        viewModelScope.launch {
                            viewModelEventSender.send {
                                it.copyToClipboard(text)
                            }
                        }
                        dismiss()
                    }

                    override fun onDismissRequest() {
                        dismiss()
                    }

                    private fun dismiss() {
                        viewModelStateFlow.update {
                            it.copy(
                                urlMenuDialog = null,
                            )
                        }
                    }
                },
            )
            viewModelStateFlow.update {
                it.copy(
                    urlMenuDialog = dialog,
                )
            }
        }

        // スマホだと長押しが効かない: 1.5.10-rc01
        override fun onLongClickUrl(text: String) {
            viewModelScope.launch {
                viewModelEventSender.send {
                    it.copyToClipboard(text)
                }
            }
        }
    }

    public interface Event {
        public fun navigateToBack()

        public fun navigateToHome()

        public fun navigate(screenStructure: ScreenStructure)

        public fun openWeb(url: String)

        public fun copyToClipboard(text: String)
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val apolloResponse: Result<ApolloResponse<ImportedMailScreenQuery.Data>>? = null,
        val confirmDialog: MailScreenUiState.AlertDialog? = null,
        val urlMenuDialog: MailScreenUiState.UrlMenuDialog? = null,
        val aiParseStartErrorMessage: String? = null,
        /**
         * 解析開始の受付後や、実行中に取得が失敗した後は apolloResponse から実行中を判定できないため、ポーリングを続ける根拠として保持する
         */
        val isAwaitingAiParseResult: Boolean = false,
        /**
         * 解析開始より前に発行した取得の応答が遅れて届くと、開始前の状態で上書きしてしまうため、開始ごとに進めて古い応答を捨てる
         */
        val aiParseStartGeneration: Int = 0,
    ) {
        fun shouldPollAiParseResult(): Boolean {
            return isAwaitingAiParseResult || isAiParseRunning()
        }

        private fun isAiParseRunning(): Boolean {
            val aiParseResult = apolloResponse?.getOrNull()?.data?.user?.importedMailAttributes?.mail?.aiParseResult
            return aiParseResult?.status == ImportedMailAiParseStatus.RUNNING
        }
    }
}
