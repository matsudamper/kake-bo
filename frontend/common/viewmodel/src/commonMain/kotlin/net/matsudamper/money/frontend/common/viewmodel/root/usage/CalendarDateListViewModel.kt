package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.CalendarDateListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.CalendarDateListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryOrderType

public class CalendarDateListViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
    private val screen: ScreenStructure.CalendarDateList,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val date: LocalDate = LocalDate(
        year = screen.year,
        monthNumber = screen.month,
        dayOfMonth = screen.day,
    )

    public val uiStateFlow: StateFlow<CalendarDateListScreenUiState> = MutableStateFlow(
        CalendarDateListScreenUiState(
            title = buildString {
                append("${date.year}/${date.monthNumber}/${date.dayOfMonth}")
                append("(${Formatter.dayOfWeekToJapanese(date.dayOfWeek)})")
            },
            loadingState = CalendarDateListScreenUiState.LoadingState.Loading,
            event = object : CalendarDateListScreenUiState.Event {
                override fun onViewInitialized() {
                    viewModelScope.launch {
                        fetch(isRefresh = true)
                    }
                }

                override fun refresh() {
                    viewModelScope.launch {
                        fetch(isRefresh = true)
                    }
                }

                override fun onClickBack() {
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.navigateBack()
                        }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val response = viewModelState.response ?: return@collectLatest
                val nodes = response.data?.user?.moneyUsages?.nodes.orEmpty()
                val hasMore = response.data?.user?.moneyUsages?.hasMore ?: true

                val items = nodes.map { result ->
                    CalendarDateListScreenUiState.Item(
                        title = result.title,
                        amount = "${Formatter.formatMoney(result.amount)}円",
                        date = Formatter.formatDateTime(result.date),
                        category = run category@{
                            val subCategory = result.moneyUsageSubCategory ?: return@category null
                            "${subCategory.category.name} / ${subCategory.name}"
                        },
                        images = result.images.map { image ->
                            CalendarDateListScreenUiState.ImageItem(url = image.url)
                        }.toImmutableList(),
                        event = object : CalendarDateListScreenUiState.ItemEvent {
                            override fun onClick() {
                                viewModelScope.launch {
                                    viewModelEventSender.send {
                                        it.navigate(ScreenStructure.MoneyUsage(id = result.id))
                                    }
                                }
                            }
                        },
                    )
                }.toImmutableList()

                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = CalendarDateListScreenUiState.LoadingState.Loaded(
                            items = items,
                            loadToEnd = hasMore.not(),
                            event = object : CalendarDateListScreenUiState.LoadedEvent {
                                override fun loadMore() {
                                    viewModelScope.launch {
                                        fetch(isRefresh = false)
                                    }
                                }
                            },
                        ),
                    )
                }
            }
        }
    }.asStateFlow()

    private suspend fun fetch(isRefresh: Boolean) {
        val cursor = if (isRefresh) {
            null
        } else {
            viewModelStateFlow.value.response?.data?.user?.moneyUsages?.cursor
        }
        if (!isRefresh && viewModelStateFlow.value.response?.data?.user?.moneyUsages?.hasMore == false) {
            return
        }

        val query = CalendarDateListScreenPagingQuery(
            query = MoneyUsagesQuery(
                cursor = Optional.present(cursor),
                size = 50,
                isAsc = true,
                orderType = Optional.present(MoneyUsagesQueryOrderType.DATE),
                filter = Optional.present(
                    MoneyUsagesQueryFilter(
                        sinceDateTime = Optional.present(
                            LocalDateTime(date, LocalTime(0, 0)),
                        ),
                        untilDateTime = Optional.present(
                            LocalDateTime(date.plus(1, DateTimeUnit.DAY), LocalTime(0, 0)),
                        ),
                    ),
                ),
            ),
        )

        val response = withContext(Dispatchers.IO) {
            graphqlClient.apolloClient.query(query)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }

        viewModelStateFlow.update { state ->
            if (isRefresh) {
                state.copy(response = response)
            } else {
                val before = state.response
                if (before == null) {
                    state.copy(response = response)
                } else {
                    val newUsages = response.data?.user?.moneyUsages ?: return@update state
                    val existingNodes = before.data?.user?.moneyUsages?.nodes.orEmpty()
                    state.copy(
                        response = response.newBuilder()
                            .data(
                                response.data?.copy(
                                    user = response.data?.user?.copy(
                                        moneyUsages = CalendarDateListScreenPagingQuery.MoneyUsages(
                                            cursor = newUsages.cursor,
                                            hasMore = newUsages.hasMore,
                                            nodes = existingNodes + newUsages.nodes,
                                        ),
                                    ),
                                ),
                            )
                            .build(),
                    )
                }
            }
        }
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
        public fun navigateBack()
    }

    private data class ViewModelState(
        val response: ApolloResponse<CalendarDateListScreenPagingQuery.Data>? = null,
    )
}
