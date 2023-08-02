package net.matsudamper.money.frontend.common.viewmodel.mail

import com.apollographql.apollo3.api.ApolloResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.mail.MailScreenUiState
import net.matsudamper.money.frontend.graphql.MailScreenQuery

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

            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                viewModelState.responseFlow.collect { apolloResponse ->
                    val mail = apolloResponse.data?.user?.importedMailAttributes?.mail ?: return@collect
                    uiStateFlow.update { uiState ->
                        uiState.copy(
                            loadingState = MailScreenUiState.LoadingState.Loaded(
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
                                                .windowed(3,3, partialWindows = true)
                                                .joinToString(",") { it.toString() }
                                            "${splitAmount}円"
                                        },
                                        category = run category@{
                                            val subCategory = suggestUsage.subCategory ?: return@category null
                                            val category = subCategory.category

                                            "${category.name} / ${subCategory.name}"
                                        },
                                        description = suggestUsage.description,
                                        dateTime = suggestUsage.date.toString(),
                                    )
                                }.toImmutableList(),
                            ),
                        )
                    }
                }
            }
        }
    }.asStateFlow()

    init {
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
        val responseFlow: Flow<ApolloResponse<MailScreenQuery.Data>> = flowOf(),
    )
}
