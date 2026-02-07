package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter
import net.matsudamper.money.frontend.graphql.updateOperation

public class MoneyUsagesListFetchModel(
    private val graphqlClient: GraphqlClient,
    private val coroutineScope: CoroutineScope,
    private val selectedMonth: LocalDate?,
) {
    private val modelStateFlow = MutableStateFlow(ModelState())
    private val resultsFlow = MutableStateFlow<ApolloResponse<UsageListScreenPagingQuery.Data>?>(null)

    private val firstQueryFlow = modelStateFlow.map {
        createQuery(
            searchText = it.searchText.orEmpty(),
            cursor = null,
            selectedMonth = selectedMonth,
        )
    }.stateIn(coroutineScope, started = SharingStarted.Lazily, initialValue = null)

    init {
        coroutineScope.launch {
            firstQueryFlow.filterNotNull().collectLatest {
                fetchData(isForceRefresh = true)
            }
        }

        coroutineScope.launch {
            modelStateFlow.mapNotNull { it.searchText }
                .map {
                    graphqlClient.apolloClient.query(getCacheQuery(it))
                        .fetchPolicy(FetchPolicy.CacheOnly)
                        .watch()
                }
                .flattenMerge()
                .collectLatest { response ->
                    resultsFlow.value = response
                }
        }
    }

    internal fun getFlow(): Flow<ApolloResponse<UsageListScreenPagingQuery.Data>> {
        return resultsFlow.filterNotNull()
    }

    internal fun fetch() {
        fetchData()
    }

    public fun changeText(searchText: String?) {
        if (modelStateFlow.value.searchText == searchText) return
        modelStateFlow.update {
            it.copy(
                searchText = searchText,
            )
        }
    }

    public fun refresh() {
        fetchData(isForceRefresh = true)
    }

    private fun fetchData(isForceRefresh: Boolean = false) {
        coroutineScope.launch {
            val firstQuery = firstQueryFlow.first() ?: return@launch
            val searchText = modelStateFlow.map { it.searchText }.first() ?: return@launch

            graphqlClient.apolloClient.updateOperation(
                cacheQueryKey = getCacheQuery(searchText = searchText),
            ) update@{ before ->
                if (before == null || isForceRefresh) {
                    val result = executeQuery(firstQuery)
                    return@update success(result)
                }
                if (before.user?.moneyUsages?.hasMore == false) return@update noHasMore()

                val cursor = before.user?.moneyUsages?.cursor ?: return@update error()
                val result = executeQuery(
                    firstQuery.copy(
                        query = firstQuery.query.copy(
                            cursor = Optional.present(cursor),
                        ),
                    ),
                )
                val newMoneyUsage = result.data?.user?.moneyUsages ?: return@update error()
                success(
                    result.newBuilder()
                        .data(
                            before.copy(
                                user = before.user?.let { user ->
                                    val usages = user.moneyUsages ?: return@let null
                                    user.copy(
                                        moneyUsages = UsageListScreenPagingQuery.MoneyUsages(
                                            cursor = newMoneyUsage.cursor,
                                            hasMore = newMoneyUsage.hasMore,
                                            nodes = usages.nodes + newMoneyUsage.nodes,
                                        ),
                                    )
                                },
                            ),
                        )
                        .build(),
                )
            }
        }
    }

    private suspend fun executeQuery(query: UsageListScreenPagingQuery): ApolloResponse<UsageListScreenPagingQuery.Data> {
        return withContext(Dispatchers.IO) {
            graphqlClient.apolloClient.query(query)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }

    private data class ModelState(
        val searchText: String? = null,
    )

    private fun getCacheQuery(searchText: String) = createQuery(
        searchText = searchText,
        cursor = null,
        selectedMonth = selectedMonth,
    )
}

private fun createQuery(
    searchText: String,
    cursor: String?,
    selectedMonth: LocalDate?,
): UsageListScreenPagingQuery {
    return UsageListScreenPagingQuery(
        query = MoneyUsagesQuery(
            cursor = Optional.present(cursor),
            size = 10,
            isAsc = if (selectedMonth != null) true else false,
            filter = Optional.present(
                MoneyUsagesQueryFilter(
                    text = Optional.present(searchText),
                    sinceDateTime = if (selectedMonth != null) {
                        Optional.present(
                            LocalDateTime(
                                LocalDate(
                                    year = selectedMonth.year,
                                    month = selectedMonth.month,
                                    dayOfMonth = 1,
                                ),
                                LocalTime(0, 0),
                            ),
                        )
                    } else {
                        Optional.absent()
                    },
                    untilDateTime = if (selectedMonth != null) {
                        Optional.present(
                            LocalDateTime(
                                LocalDate(
                                    year = selectedMonth.year,
                                    monthNumber = selectedMonth.monthNumber,
                                    dayOfMonth = 1,
                                ).plus(1, DateTimeUnit.MONTH),
                                LocalTime(0, 0),
                            ),
                        )
                    } else {
                        Optional.absent()
                    },
                ),
            ),
        ),
    )
}
