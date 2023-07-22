package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import MailScreenUiState
import net.matsudamper.money.element.MailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GetMailQuery
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi

public class MailImportViewModel(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val graphqlApi: MailImportScreenGraphqlApi,
    private val loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelEventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val rootUiStateFlow: StateFlow<MailScreenUiState> = MutableStateFlow(
        MailScreenUiState(
            isLoading = true,
            htmlDialog = null,
            mails = immutableListOf(),
            event = object : MailScreenUiState.Event {
                override fun htmlDismissRequest() {
                    viewModelStateFlow.update {
                        it.copy(html = null)
                    }
                }

                override fun onViewInitialized() {
                    fetch()
                }

                override fun onClickImport() {
                    import()
                }

                override fun onClickBackButton() {
                    coroutineScope.launch {
                        viewModelEventSender.send { it.backRequest() }
                    }
                }
            },
        ),
    ).also {
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                it.update {
                    it.copy(
                        isLoading = viewModelState.isLoading,
                        mails = viewModelState.usrMails.map { mail ->
                            MailScreenUiState.Mail(
                                subject = mail.subject.replace("\n", ""),
                                isSelected = mail.id in viewModelState.checked,
                                sender = mail.sender,
                                from = mail.from.joinToString(","),
                                event = createMailItemEvent(mail = mail),
                            )
                        }.toImmutableList(),
                        htmlDialog = viewModelState.html,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createMailItemEvent(mail: GetMailQuery.UsrMail): MailScreenUiState.Mail.Event {
        return object : MailScreenUiState.Mail.Event {
            override fun onClick() {
                viewModelStateFlow.update { viewModelState ->
                    val isChecked = mail.id in viewModelState.checked
                    viewModelState.copy(
                        checked = run {
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
        }
    }

    private fun fetch() {
        coroutineScope.launch {
            val result = loginCheckUseCase.check()
            if (result) {
                viewModelStateFlow.update {
                    it.copy(isLoading = false)
                }
            } else {
                return@launch
            }

            val mails = runCatching {
                withContext(ioDispatcher) {
                    graphqlApi.getMail()
                }
            }.getOrNull() ?: return@launch

            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    usrMails = mails.data?.user?.userMailAttributes?.mail?.usrMails.orEmpty(),
                    checked = mails.data?.user?.userMailAttributes?.mail?.usrMails.orEmpty().map { it.id },
                )
            }
        }
    }

    private fun import() {
        // TODO
    }

    public interface Event {
        public fun backRequest()
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val usrMails: List<GetMailQuery.UsrMail> = listOf(),
        val checked: List<MailId> = listOf(),
        val html: String? = null,
    )
}
