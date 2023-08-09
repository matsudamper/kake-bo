package net.matsudamper.money.frontend.common.viewmodel.moneyusage

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.moneyusage.MoneyUsageScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class MoneyUsageScreenViewModel(
    coroutineScope: CoroutineScope,
    id: MoneyUsageId,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiStateFlow: StateFlow<MoneyUsageScreenUiState> = MutableStateFlow(
        MoneyUsageScreenUiState(
            event = object : MoneyUsageScreenUiState.Event {
                override fun onViewInitialized() {
                    coroutineScope.launch {
                        apolloCollector.fetch(this)
                    }
                }

                override fun onClickRetry() {
                    coroutineScope.launch {
                        apolloCollector.fetch(this)
                    }
                }
            },
            loadingState = MoneyUsageScreenUiState.LoadingState.Loading,
        )
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val loadingState = run loadingState@{
                    when (val state = viewModelState.apolloResponseState) {
                        is ApolloResponseState.Failure -> {
                            MoneyUsageScreenUiState.LoadingState.Error
                        }

                        is ApolloResponseState.Loading -> {
                            MoneyUsageScreenUiState.LoadingState.Loading
                        }

                        is ApolloResponseState.Success -> {
                            val moneyUsage = state.value.data?.user?.moneyUsage
                                ?: return@loadingState MoneyUsageScreenUiState.LoadingState.Error

                            MoneyUsageScreenUiState.LoadingState.Loaded(
                                moneyUsage = MoneyUsageScreenUiState.MoneyUsage(
                                    title = moneyUsage.title,
                                    amount = "${Formatter.formatMoney(moneyUsage.amount)}円",
                                    description = moneyUsage.description,
                                    dateTime = moneyUsage.date.toString(),
                                    category = run category@{
                                        val subCategory = moneyUsage.moneyUsageSubCategory ?: return@category "未指定"
                                        val category = subCategory.category
                                        "${subCategory.name} / ${category.name}"
                                    },
                                    event = object : MoneyUsageScreenUiState.MoneyUsageEvent {
                                        override fun onClickTitleChange() {
                                            // TODO
                                        }

                                        override fun onClickCategoryChange() {
                                            // TODO
                                        }

                                        override fun onClickDateChange() {
                                            // TODO
                                        }

                                        override fun onClickDescription() {
                                            // TODO
                                        }
                                    }
                                ),
                                linkedMails = moneyUsage.linkedMail.orEmpty().map { mail ->
                                    MoneyUsageScreenUiState.MailItem(
                                        subject = mail.subject,
                                        from = mail.from,
                                        date = mail.dateTime.toString(),
                                        event = object : MoneyUsageScreenUiState.MailItemEvent {
                                            override fun onClick() {
                                                coroutineScope.launch {
                                                    eventSender.send {
                                                        it.navigate(ScreenStructure.ImportedMail(id = mail.id))
                                                    }
                                                }
                                            }
                                        },
                                    )
                                }.toImmutableList(),
                                event = object : MoneyUsageScreenUiState.LoadedEvent {
                                    override fun onClickDelete() {
                                        // TODO
                                    }
                                }
                            )
                        }
                    }
                }

                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = loadingState,
                    )
                }
            }
        }
    }.asStateFlow()

    private val apolloCollector = ApolloResponseCollector.create(
        apolloClient = apolloClient,
        query = MoneyUsageScreenQuery(
            id = id,
        )
    )

    init {
        coroutineScope.launch {
            apolloCollector.flow.collectLatest { apolloResponseState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        apolloResponseState = apolloResponseState,
                    )
                }
            }
        }
    }

    public interface Event {
        public fun navigate(structure: ScreenStructure)
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponseState<ApolloResponse<MoneyUsageScreenQuery.Data>> = ApolloResponseState.loading(),
    )
}
