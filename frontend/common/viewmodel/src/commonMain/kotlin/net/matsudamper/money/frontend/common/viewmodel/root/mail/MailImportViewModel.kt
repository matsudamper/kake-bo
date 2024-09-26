package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.money.element.MailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportMailScreenUiState
import net.matsudamper.money.frontend.common.usecase.LoginCheckUseCaseImpl
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GetMailQuery
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi
import net.matsudamper.money.frontend.graphql.type.DeleteMailResultError

public class MailImportViewModel(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val graphqlApi: MailImportScreenGraphqlApi,
    private val loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
) {
    private val viewModelEventSender = EventSender<MailImportViewModelEvent>()
    public val eventHandler: EventHandler<MailImportViewModelEvent> = viewModelEventSender.asHandler()

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val rootUiStateFlow: StateFlow<ImportMailScreenUiState> =
        MutableStateFlow(
            ImportMailScreenUiState(
                isLoading = true,
                htmlDialog = null,
                mails = immutableListOf(),
                showLoadMore = false,
                mailDeleteDialog = null,
                event =
                object : ImportMailScreenUiState.Event {
                    override fun htmlDismissRequest() {
                        viewModelStateFlow.update {
                            it.copy(html = null)
                        }
                    }

                    override fun onViewInitialized() {
                        coroutineScope.launch {
                            val result = loginCheckUseCase.check()
                            if (result.not()) return@launch
                            fetch()
                        }
                    }

                    override fun onClickImport() {
                        import()
                    }

                    override fun onClickLoadMore() {
                        fetch()
                    }
                },
            ),
        ).also {
            coroutineScope.launch {
                viewModelStateFlow.collect { viewModelState ->
                    it.update {
                        it.copy(
                            isLoading = viewModelState.isLoading,
                            showLoadMore = viewModelState.finishLoadingToEnd == false,
                            mails =
                            viewModelState.usrMails.map { mail ->
                                ImportMailScreenUiState.Mail(
                                    subject = mail.subject.replace("\n", ""),
                                    isSelected = mail.id in viewModelState.checked,
                                    sender = mail.sender,
                                    from = mail.from.joinToString(","),
                                    event = createMailItemEvent(mail = mail),
                                )
                            }.toImmutableList(),
                            htmlDialog = viewModelState.html,
                            mailDeleteDialog =
                            run {
                                val dialogState = viewModelState.mailDeleteDialogState
                                if (dialogState == null) {
                                    null
                                } else {
                                    ImportMailScreenUiState.MailDeleteDialog(
                                        errorText = dialogState.errorText,
                                        event = createMailDeleteDialogEvent(dialogState.mail),
                                        isLoading = dialogState.isLoading,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }.asStateFlow()

    private fun createMailDeleteDialogEvent(mailDeleteDialog: GetMailQuery.UsrMail): ImportMailScreenUiState.MailDeleteDialog.Event {
        return object : ImportMailScreenUiState.MailDeleteDialog.Event {
            override fun onClickDelete() {
                coroutineScope.launch {
                    viewModelStateFlow.update {
                        it.copy(
                            mailDeleteDialogState =
                            it.mailDeleteDialogState?.copy(
                                isLoading = true,
                            ),
                        )
                    }

                    val result = graphqlApi.deleteMail(listOf(mailDeleteDialog.id))

                    if (result?.data?.userMutation?.deleteMail?.isSuccess == true) {
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                mailDeleteDialogState = null,
                                isLoading = false,
                                usrMails =
                                viewModelState.usrMails.filterNot { mail ->
                                    mail.id == mailDeleteDialog.id
                                },
                            )
                        }
                    } else {
                        val errorText =
                            when (result?.data?.userMutation?.deleteMail?.error) {
                                DeleteMailResultError.InternalServerError,
                                DeleteMailResultError.UNKNOWN__,
                                null,
                                -> "サーバーエラーが発生しました"

                                DeleteMailResultError.MailConfigNotFound -> "メール設定がされていません"
                                DeleteMailResultError.MailServerNotConnected -> "メールサーバーに接続できませんでした"
                            }
                        viewModelStateFlow.update {
                            it.copy(
                                mailDeleteDialogState =
                                it.mailDeleteDialogState?.copy(
                                    errorText = errorText,
                                    isLoading = false,
                                ),
                            )
                        }
                    }
                }
            }

            override fun onClickCancel() {
                onDismiss()
            }

            override fun onDismiss() {
                viewModelStateFlow.update {
                    it.copy(
                        mailDeleteDialogState = null,
                    )
                }
            }
        }
    }

    private fun createMailItemEvent(mail: GetMailQuery.UsrMail): ImportMailScreenUiState.Mail.Event {
        return object : ImportMailScreenUiState.Mail.Event {
            override fun onClick() {
                viewModelStateFlow.update { viewModelState ->
                    val isChecked = mail.id in viewModelState.checked
                    viewModelState.copy(
                        checked =
                        run {
                            if (isChecked) {
                                viewModelState.checked - mail.id
                            } else {
                                viewModelState.checked + mail.id
                            }
                        },
                    )
                }
            }

            override fun onClickDetail() {
                viewModelStateFlow.update {
                    it.copy(html = mail.html)
                }
            }

            override fun onClickDelete() {
                viewModelStateFlow.update {
                    it.copy(
                        mailDeleteDialogState =
                        ViewModelState.MailDelete(
                            mail = mail,
                            errorText = null,
                            isLoading = false,
                        ),
                    )
                }
            }
        }
    }

    private var fetchJob = Job()

    private fun fetch() {
        if (viewModelStateFlow.value.finishLoadingToEnd == true) return
        fetchJob.cancel()
        coroutineScope.launch(
            Job().also { fetchJob = it },
        ) {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    isLoading = true,
                )
            }

            val mails =
                try {
                    runCatching {
                        withContext(ioDispatcher) {
                            graphqlApi.getMail(viewModelStateFlow.value.cursor)
                        }
                    }.getOrNull() ?: return@launch
                } finally {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            isLoading = false,
                        )
                    }
                }

            val mail = mails.data?.user?.userMailAttributes?.mails ?: return@launch

            if (isActive.not()) return@launch
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    usrMails = viewModelState.usrMails + mail.usrMails,
                    cursor = mail.cursor,
                    finishLoadingToEnd = mail.cursor == null,
                    isLoading = false,
                )
            }
        }
    }

    private fun import() {
        coroutineScope.launch {
            val result =
                runCatching {
                    graphqlApi.mailImport(viewModelStateFlow.value.checked)
                }.getOrNull()
            val onError =
                suspend {
                    viewModelEventSender.send { it.globalToast("Importに失敗しました") }
                }

            if (result == null) {
                onError()
                return@launch
            }

            if (result.data?.userMutation?.importMail?.isSuccess != true) {
                onError()
                return@launch
            }

            viewModelStateFlow.update {
                it.copy(
                    usrMails = listOf(),
                    cursor = null,
                    checked = listOf(),
                    finishLoadingToEnd = null,
                )
            }
            fetch()
        }
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val usrMails: List<GetMailQuery.UsrMail> = listOf(),
        val cursor: String? = null,
        val checked: List<MailId> = listOf(),
        val finishLoadingToEnd: Boolean? = null,
        val html: String? = null,
        val mailDeleteDialogState: MailDelete? = null,
    ) {
        data class MailDelete(
            val mail: GetMailQuery.UsrMail,
            val errorText: String?,
            val isLoading: Boolean,
        )
    }
}

public interface MailImportViewModelEvent {
    public fun globalToast(message: String)
}
