package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.AddMoneyUsageMutation
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.AddUsageQuery

public class AddMoneyUsageScreenApi(
    private val graphqlClient: GraphqlClient,
) {
    public suspend fun addMoneyUsage(
        title: String,
        description: String,
        amount: Int,
        datetime: LocalDateTime,
        subCategoryId: MoneyUsageSubCategoryId?,
        importedMailId: ImportedMailId?,
    ): ApolloResponse<AddMoneyUsageMutation.Data>? {
        return runCatching {
            graphqlClient.apolloClient
                .mutation(
                    AddMoneyUsageMutation(
                        AddUsageQuery(
                            subCategoryId = Optional.present(subCategoryId),
                            title = title,
                            description = description,
                            amount = amount,
                            date = datetime,
                            importedMailId = Optional.present(importedMailId),
                        ),
                    ),
                )
                .execute()
        }.getOrNull()
    }

    public suspend fun get(id: ImportedMailId): Result<ApolloResponse<AddMoneyUsageScreenQuery.Data>> {
        return runCatching {
            graphqlClient.apolloClient
                .query(
                    AddMoneyUsageScreenQuery(
                        id = id,
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }
}
