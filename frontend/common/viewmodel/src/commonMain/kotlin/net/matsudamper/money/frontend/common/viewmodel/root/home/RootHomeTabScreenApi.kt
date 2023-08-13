package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsageStaticsQuery

public class RootHomeTabScreenApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun fetch(
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int,
    ): Result<ApolloResponse<RootHomeTabScreenQuery.Data>> {
        return runCatching {
            apolloClient.query(
                RootHomeTabScreenQuery(
                    query = MoneyUsageStaticsQuery(
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
            ).execute()
        }
    }
}
