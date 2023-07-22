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
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.graphql.GetMailQuery
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi

public class MailImportViewModel(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val graphqlApi: MailImportScreenGraphqlApi,
    private val loginCheckUseCase: LoginCheckUseCase,
) {
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
                                subject = buildString {
                                    appendLine(mail.subject)
                                    appendLine("sender: ${mail.sender}")
                                    appendLine("from: ${mail.from.joinToString("\n")}")
                                },
                                text = mail.plain.orEmpty(),
                                onClick = {
                                    viewModelStateFlow.update { viewModelState ->
                                        viewModelState.copy(
                                            html = mail.html ?: mail.plain,
                                        )
                                    }
                                },
                            )
                        }.toImmutableList(),
                        htmlDialog = viewModelState.html,
                    )
                }
            }
        }
    }.asStateFlow()

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

            viewModelStateFlow.update {
                it.copy(
                    usrMails = mails.data?.user?.userMailAttributes?.mail?.usrMails.orEmpty(),
                )
            }
        }
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val usrMails: List<GetMailQuery.UsrMail> = listOf(),
        val html: String? = null,
    )
}
