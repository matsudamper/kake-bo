package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageCalendarScreenPagingQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter
import net.matsudamper.money.frontend.graphql.updateOperation

public class RootUsageCalendarPagingModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlClient: GraphqlClient,
) {
    private val modelStateFlow = MutableStateFlow(ModelState())

    private val firstQuery = modelStateFlow.map {
        createQuery(
            selectedMonth = it.selectedMonth ?: return@map null,
            searchText = it.searchText.orEmpty(),
            cursor = null,
        )
    }.stateIn(coroutineScope, started = SharingStarted.Lazily, initialValue = null)

    init {
        coroutineScope.launch {
            firstQuery.filterNotNull().collectLatest {
                fetchData(isForceRefresh = true)
            }
        }
    }

    public fun changeMonth(month: LocalDate) {
        println("changeMonth: $month")
        modelStateFlow.update {
            it.copy(
                selectedMonth = month,
            )
        }
    }

    public fun changeSearchText(text: String?) {
        if (modelStateFlow.value.searchText == text) return
        modelStateFlow.update {
            it.copy(
                searchText = text,
            )
        }
    }

    public fun hasSelectedMonth(): Boolean {
        return modelStateFlow.value.selectedMonth != null
    }

    public fun refresh() {
        fetchData(isForceRefresh = true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun getFlow(): Flow<ApolloResponse<UsageCalendarScreenPagingQuery.Data>> {
        return graphqlClient.apolloClient.query(cacheQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .watch()
    }

    internal fun fetch() {
        fetchData()
    }

    private fun fetchData(isForceRefresh: Boolean = false) {
        coroutineScope.launch {
            val query = firstQuery.first() ?: return@launch
            graphqlClient.apolloClient.updateOperation(cacheQuery) { before ->
                if (before == null || isForceRefresh) return@updateOperation executeQuery(query)
                if (before.user?.moneyUsages?.hasMore == false) return@updateOperation null

                val cursor = before.user?.moneyUsages?.cursor ?: return@updateOperation null
                val result = executeQuery(
                    query.copy(
                        query = query.query.copy(
                            cursor = Optional.present(cursor),
                        ),
                    ),
                )
                val newMoneyUsage = result.data?.user?.moneyUsages ?: return@updateOperation null
                result.newBuilder()
                    .data(
                        before.copy(
                            user = before.user?.let { user ->
                                val usages = user.moneyUsages ?: return@let null
                                user.copy(
                                    moneyUsages = UsageCalendarScreenPagingQuery.MoneyUsages(
                                        cursor = newMoneyUsage.cursor,
                                        hasMore = newMoneyUsage.hasMore,
                                        nodes = usages.nodes + newMoneyUsage.nodes,
                                    ),
                                )
                            },
                        ),
                    )
                    .build()
            }
        }
    }

    private suspend fun executeQuery(query: UsageCalendarScreenPagingQuery): ApolloResponse<UsageCalendarScreenPagingQuery.Data> {
        return withContext(Dispatchers.IO) {
            graphqlClient.apolloClient.query(query)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }

    private data class ModelState(
        val selectedMonth: LocalDate? = null,
        val searchText: String? = null,
    )

    public companion object {
        private val cacheQuery = createQuery(
            selectedMonth = LocalDate.Companion.fromEpochDays(0),
            searchText = "",
            cursor = null,
        )
    }
}

private fun createQuery(
    selectedMonth: LocalDate,
    searchText: String,
    cursor: String?,
): UsageCalendarScreenPagingQuery {
    return UsageCalendarScreenPagingQuery(
        query = MoneyUsagesQuery(
            cursor = Optional.present(cursor),
            filter = Optional.present(
                MoneyUsagesQueryFilter(
                    sinceDateTime = Optional.present(
                        LocalDateTime(
                            LocalDate(
                                year = selectedMonth.year,
                                month = selectedMonth.month,
                                dayOfMonth = 1,
                            ),
                            LocalTime(0, 0),
                        ),
                    ),
                    untilDateTime = Optional.present(
                        LocalDateTime(
                            LocalDate(
                                year = selectedMonth.year,
                                monthNumber = selectedMonth.monthNumber,
                                dayOfMonth = 1,
                            ).plus(1, DateTimeUnit.MONTH),
                            LocalTime(0, 0),
                        ),
                    ),
                    text = Optional.present(searchText),
                ),
            ),
            isAsc = true,
            size = 50,
        ),
    )
}
