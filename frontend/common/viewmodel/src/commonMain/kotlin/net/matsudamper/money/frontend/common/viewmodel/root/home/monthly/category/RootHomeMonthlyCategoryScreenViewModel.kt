package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeMonthlyCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MonthlyCategoryScreenListQuery
import net.matsudamper.money.frontend.graphql.MonthlyCategoryScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class RootHomeMonthlyCategoryScreenViewModel(
    private val coroutineScope: CoroutineScope,
    argument: RootHomeScreenStructure.MonthlyCategory,
    loginCheckUseCase: LoginCheckUseCase,
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            year = argument.year,
            month = argument.month,
            categoryId = argument.categoryId,
        ),
    )
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val monthlyCategoryResultState: ApolloPagingResponseCollector<MonthlyCategoryScreenListQuery.Data> = ApolloPagingResponseCollector.create(
        apolloClient = apolloClient,
        fetchPolicy = FetchPolicy.CacheFirst,
        coroutineScope = coroutineScope,
    )

    public val uiStateFlow: StateFlow<RootHomeMonthlyCategoryScreenUiState> = MutableStateFlow(
        RootHomeMonthlyCategoryScreenUiState(
            event = object : RootHomeMonthlyCategoryScreenUiState.Event {
                override fun onViewInitialized() {
                    coroutineScope.launch {
                        loginCheckUseCase.check()
                    }
                    coroutineScope.launch {
                        monthlyCategoryResultState.flow.collectLatest { results ->
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    apolloResponses = results,
                                )
                            }
                        }
                    }
                    coroutineScope.launch {
                        viewModelStateFlow.map { viewModelState ->
                            viewModelState.categoryId
                        }.stateIn(this).collectLatest {
                            val collector = ApolloResponseCollector.create(
                                apolloClient = apolloClient,
                                fetchPolicy = FetchPolicy.CacheFirst,
                                query = MonthlyCategoryScreenQuery(
                                    id = it,
                                ),
                            )
                            collector.fetch(this)
                            collector.flow.collectLatest { responseState ->
                                val categoryName = responseState.getSuccessOrNull()?.value?.data?.user?.moneyUsageCategory?.name
                                viewModelStateFlow.update { viewModelState ->
                                    viewModelState.copy(
                                        categoryName = categoryName,
                                    )
                                }
                            }
                        }
                    }
                    fetch()
                }
            },
            loadingState = RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading,
            title = "",
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                viewModelState.categoryId
                val state = when (viewModelState.apolloResponses.firstOrNull()) {
                    is ApolloResponseState.Failure -> {
                        RootHomeMonthlyCategoryScreenUiState.LoadingState.Error
                    }

                    null,
                    is ApolloResponseState.Loading,
                    -> {
                        RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading
                    }

                    is ApolloResponseState.Success -> {
                        RootHomeMonthlyCategoryScreenUiState.LoadingState.Loaded(
                            items = viewModelState.apolloResponses.flatMap {
                                it.getSuccessOrNull()?.value?.data?.user?.moneyUsages?.nodes.orEmpty()
                            }.map {
                                RootHomeMonthlyCategoryScreenUiState.Item(
                                    title = it.title,
                                    amount = "${Formatter.formatMoney(it.amount)}円",
                                    subCategory = it.moneyUsageSubCategory?.name.orEmpty(),
                                )
                            },
                        )
                    }
                }
                uiStateFlow.value = uiStateFlow.value.copy(
                    loadingState = state,
                    title = run {
                        val yearText = "${viewModelState.year}年${viewModelState.month}月"
                        val descriptionText = if (viewModelState.categoryName == null) {
                            "カテゴリ別一覧"
                        } else {
                            "${viewModelState.categoryName}"
                        }
                        "$yearText $descriptionText"
                    },
                )
            }
        }
    }

    public fun updateStructure(current: RootHomeScreenStructure.MonthlyCategory) {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                year = current.year,
                month = current.month,
                categoryId = current.categoryId,
            )
        }
        monthlyCategoryResultState.clear()
        fetch()
    }

    private fun fetch() {
        monthlyCategoryResultState.add { results ->
            when (val lastResponseState = results.lastOrNull()?.flow?.value) {
                null -> {
                    val date = LocalDate(
                        year = viewModelStateFlow.value.year,
                        monthNumber = viewModelStateFlow.value.month,
                        dayOfMonth = 1,
                    )
                    MonthlyCategoryScreenListQuery(
                        cursor = Optional.present(null),
                        size = 50,
                        category = viewModelStateFlow.value.categoryId,
                        sinceDateTime = Optional.present(
                            LocalDateTime(
                                date = date,
                                time = LocalTime(0, 0),
                            ),
                        ),
                        untilDateTime = Optional.present(
                            LocalDateTime(
                                date = date.plus(1, DateTimeUnit.MONTH),
                                time = LocalTime(0, 0),
                            ),
                        ),
                    )
                }

                is ApolloResponseState.Success -> {
                    val moneyUsage = lastResponseState.value.data?.user?.moneyUsages ?: return@add null
                    if (moneyUsage.hasMore) {
                        MonthlyCategoryScreenListQuery(
                            cursor = Optional.present(moneyUsage.cursor),
                            size = 50,
                            category = viewModelStateFlow.value.categoryId,
                        )
                    } else {
                        null
                    }
                }

                is ApolloResponseState.Failure,
                is ApolloResponseState.Loading,
                -> return@add null
            }
        }
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val year: Int,
        val month: Int,
        val categoryId: MoneyUsageCategoryId,
        val categoryName: String? = null,
        val apolloResponses: List<ApolloResponseState<ApolloResponse<MonthlyCategoryScreenListQuery.Data>>> = listOf(),
    )
}
