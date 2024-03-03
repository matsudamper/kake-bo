package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
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
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageCalendarScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter

public class RootUsageCalendarPagingModel(
    private val coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val modelStateFlow = MutableStateFlow(ModelState())

    private val pagingFlow =
        MutableStateFlow(
            ApolloPagingResponseCollector.create<UsageCalendarScreenPagingQuery.Data>(
                apolloClient = apolloClient,
                coroutineScope = coroutineScope,
            ),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun getFlow(): Flow<List<ApolloResponseState<ApolloResponse<UsageCalendarScreenPagingQuery.Data>>>> {
        return flow {
            emitAll(
                pagingFlow.mapLatest { collectors ->
                    collectors.getFlow()
                }.flattenMerge()
                    .onEach {
                        println("CalendarPaging: ${it.size}")
                    },
            )
        }
    }

    internal fun fetch() {
        fetch(
            selectedMonth = modelStateFlow.value.selectedMonth,
            searchText = modelStateFlow.value.searchText,
        )
    }

    private fun fetch(
        selectedMonth: LocalDate?,
        searchText: String?,
    ) {
        selectedMonth ?: return
        pagingFlow.value.add { responseStateList ->
            val cursor: String?
            when (val lastState = responseStateList.lastOrNull()?.flow?.value) {
                is ApolloResponseState.Loading -> return@add null
                is ApolloResponseState.Failure -> {
                    coroutineScope.launch {
                        pagingFlow.value.lastRetry()
                    }
                    return@add null
                }

                null -> {
                    cursor = null
                }

                is ApolloResponseState.Success -> {
                    val result = lastState.value.data?.user?.moneyUsages
                    if (result == null) {
                        coroutineScope.launch {
                            pagingFlow.value.lastRetry()
                        }
                        return@add null
                    }
                    if (result.hasMore.not()) return@add null

                    cursor = result.cursor
                }
            }
            println("cursor: $cursor")
            UsageCalendarScreenPagingQuery(
                query =
                    MoneyUsagesQuery(
                        cursor = Optional.present(cursor),
                        filter =
                            Optional.present(
                                MoneyUsagesQueryFilter(
                                    sinceDateTime =
                                        Optional.present(
                                            LocalDateTime(
                                                LocalDate(
                                                    year = selectedMonth.year,
                                                    month = selectedMonth.month,
                                                    dayOfMonth = 1,
                                                ),
                                                LocalTime(0, 0),
                                            ),
                                        ),
                                    untilDateTime =
                                        Optional.present(
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
    }

    public fun changeMonth(month: LocalDate) {
        println("changeMonth: $month")
        modelStateFlow.update {
            it.copy(
                selectedMonth = month,
            )
        }
        pagingFlow.value =
            ApolloPagingResponseCollector.create(
                apolloClient = apolloClient,
                coroutineScope = coroutineScope,
            )
    }

    public fun changeSearchText(text: String?) {
        if (modelStateFlow.value.searchText == text) return
        modelStateFlow.update {
            it.copy(
                searchText = text,
            )
        }
        pagingFlow.value =
            ApolloPagingResponseCollector.create(
                apolloClient = apolloClient,
                coroutineScope = coroutineScope,
            )
    }

    public fun hasSelectedMonth(): Boolean {
        return modelStateFlow.value.selectedMonth != null
    }

    private data class ModelState(
        val selectedMonth: LocalDate? = null,
        val searchText: String? = null,
    )
}
