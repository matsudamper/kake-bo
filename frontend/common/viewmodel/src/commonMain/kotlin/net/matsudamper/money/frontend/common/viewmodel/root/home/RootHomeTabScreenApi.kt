package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByCategoryQuery
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByDateQuery
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsageAnalyticsQuery

public class RootHomeTabScreenApi(
    private val graphqlClient: GraphqlClient,
) {
    public fun screenFlow(): Flow<ApolloResponse<RootHomeTabScreenQuery.Data>> {
        return graphqlClient.apolloClient
            .query(RootHomeTabScreenQuery())
            .toFlow()
    }

    public suspend fun fetchCategory(
        id: MoneyUsageCategoryId,
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int,
        useCache: Boolean,
    ): Result<ApolloResponse<RootHomeTabScreenAnalyticsByCategoryQuery.Data>> {
        return runCatching {
            graphqlClient.apolloClient.query(
                RootHomeTabScreenAnalyticsByCategoryQuery(
                    id = id,
                    query = MoneyUsageAnalyticsQuery(
                        sinceDateTime = LocalDateTime(
                            LocalDate(startYear, startMonth, 1),
                            LocalTime(0, 0, 0),
                        ),
                        untilDateTime = LocalDateTime(
                            LocalDate(endYear, endMonth, 1),
                            LocalTime(0, 0, 0),
                        ),
                    ),
                ),
            )
                .fetchPolicy(
                    if (useCache) {
                        FetchPolicy.CacheFirst
                    } else {
                        FetchPolicy.NetworkOnly
                    },
                )
                .execute()
        }.onFailure {
            it.printStackTrace()
        }
    }

    public fun fetchAll(
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int,
        useCache: Boolean,
    ): Result<Flow<ApolloResponse<RootHomeTabScreenAnalyticsByDateQuery.Data>>> {
        return runCatching {
            graphqlClient.apolloClient
                .query(
                    RootHomeTabScreenAnalyticsByDateQuery(
                        query = MoneyUsageAnalyticsQuery(
                            sinceDateTime = LocalDateTime(
                                LocalDate(startYear, startMonth, 1),
                                LocalTime(0, 0, 0),
                            ),
                            untilDateTime = LocalDateTime(
                                LocalDate(endYear, endMonth, 1),
                                LocalTime(0, 0, 0),
                            ),
                        ),
                    ),
                )
                .fetchPolicy(
                    if (useCache) {
                        FetchPolicy.CacheFirst
                    } else {
                        FetchPolicy.NetworkOnly
                    },
                )
                .toFlow()
        }.onFailure {
            it.printStackTrace()
        }
    }
}
