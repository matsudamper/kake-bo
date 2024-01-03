package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageCalendarScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter

public class RootUsageCalendarPagingModel(
    private val coroutineScope: CoroutineScope,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val modelStateFlow = MutableStateFlow(ModelState())

    private val paging = ApolloPagingResponseCollector.create<UsageCalendarScreenPagingQuery.Data>(
        apolloClient = apolloClient,
    )

    internal val flow = paging.flow

    internal suspend fun fetch() {
        val selectedMonth = modelStateFlow.value.selectedMonth ?: return
        val searchText = modelStateFlow.value.searchText
        paging.add { collectors ->
            val cursor: String?
            when (val lastState = collectors.lastOrNull()?.flow?.value) {
                is ApolloResponseState.Loading -> return@add null
                is ApolloResponseState.Failure -> {
                    coroutineScope.launch {
                        paging.lastRetry()
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
                            paging.lastRetry()
                        }
                        return@add null
                    }
                    if (result.hasMore.not()) return@add null

                    cursor = result.cursor
                }
            }
            UsageCalendarScreenPagingQuery(
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
    }

    public fun changeMonth(
        month: LocalDate,
    ) {
        modelStateFlow.update {
            it.copy(
                selectedMonth = month,
            )
        }
        paging.clear()
    }

    public fun changeSearchText(
        text: String?,
    ) {
        if (modelStateFlow.value.searchText == text) return
        modelStateFlow.update {
            it.copy(
                searchText = text,
            )
        }
        paging.clear()
    }

    private data class ModelState(
        val selectedMonth: LocalDate? = null,
        val searchText: String? = null,
    )
}
