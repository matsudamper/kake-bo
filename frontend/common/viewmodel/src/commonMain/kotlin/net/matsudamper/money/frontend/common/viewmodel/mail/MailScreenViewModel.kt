package net.matsudamper.money.frontend.common.viewmodel.mail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.mail.MailScreenUiState
import net.matsudamper.money.frontend.graphql.MailScreenQuery
import net.matsudamper.money.lib.ResultWrapper

public class MailScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: MailScreenGraphqlApi,
    private val importedMailId: ImportedMailId,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<MailScreenUiState> = MutableStateFlow(
        MailScreenUiState(
            loadingState = MailScreenUiState.LoadingState.Loading,
            event = object : MailScreenUiState.Event {
                override fun onClickRetry() {
                    fetch()
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                viewModelState.responseFlow
                    .stateIn(this, SharingStarted.Lazily, null)
                    .collect { mailResponseWrapper ->
                        uiStateFlow.update { uiState ->
                            uiState.copy(
                                loadingState = when (mailResponseWrapper) {
                                    null -> {
                                        MailScreenUiState.LoadingState.Loading
                                    }

                                    is ResultWrapper.Failure -> {
                                        MailScreenUiState.LoadingState.Error
                                    }

                                    is ResultWrapper.Success -> {
                                        val mail = mailResponseWrapper.value.data?.user?.importedMailAttributes?.mail
                                        if (mail == null) {
                                            MailScreenUiState.LoadingState.Loading
                                        } else {
                                            MailScreenUiState.LoadingState.Loaded(
                                                mail = MailScreenUiState.Mail(
                                                    title = mail.subject,
                                                    date = mail.dateTime.toString(),
                                                    from = mail.from,
                                                ),
                                                usageSuggest = mail.suggestUsages.map { suggestUsage ->
                                                    MailScreenUiState.UsageSuggest(
                                                        title = suggestUsage.title,
                                                        amount = run amount@{
                                                            val amount = suggestUsage.amount ?: return@amount null

                                                            val splitAmount = amount.toString().toList()
                                                                .reversed()
                                                                .windowed(3, 3, partialWindows = true)
                                                                .map { it.reversed() }
                                                                .reversed()
                                                                .joinToString(",") { it.joinToString("") }
                                                            "${splitAmount}円"
                                                        },
                                                        category = run category@{
                                                            val subCategory =
                                                                suggestUsage.subCategory ?: return@category null
                                                            val category = subCategory.category

                                                            "${category.name} / ${subCategory.name}"
                                                        },
                                                        description = suggestUsage.description,
                                                        dateTime = suggestUsage.dateTime.toString(),
                                                    )
                                                }.toImmutableList(),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
            }
        }
    }.asStateFlow()

    init {
        fetch()
    }

    private fun fetch() {
        coroutineScope.launch {
            val responseFlow = api.get(id = importedMailId)
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    responseFlow = responseFlow,
                )
            }
        }
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val responseFlow: Flow<ResultWrapper<ApolloResponse<MailScreenQuery.Data>>> = flowOf(),
    )
}
