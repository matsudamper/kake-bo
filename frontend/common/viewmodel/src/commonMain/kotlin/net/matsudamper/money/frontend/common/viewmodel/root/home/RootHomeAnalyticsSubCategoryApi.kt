package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsBySubCategoryQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsageAnalyticsQuery

public class RootHomeAnalyticsSubCategoryApi(
    private val graphqlClient: GraphqlClient,
) {
    public fun watch(
        subCategory: MoneyUsageSubCategoryId,
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int,
        useCache: Boolean,
    ): Flow<ApolloResponse<RootHomeTabScreenAnalyticsBySubCategoryQuery.Data>> {
        return graphqlClient.apolloClient
            .query(
                RootHomeTabScreenAnalyticsBySubCategoryQuery(
                    id = subCategory,
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
            .watch()
    }
}
