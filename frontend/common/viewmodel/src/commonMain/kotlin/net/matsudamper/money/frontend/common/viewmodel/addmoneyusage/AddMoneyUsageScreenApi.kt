package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.AddMoneyUsageMutation
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.AddUsageQuery

public class AddMoneyUsageScreenApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun addMoneyUsage(
        title: String,
        description: String,
        amount: Int,
        datetime: LocalDateTime,
        subCategoryId: MoneyUsageSubCategoryId?,
    ): ApolloResponse<AddMoneyUsageMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    AddMoneyUsageMutation(
                        AddUsageQuery(
                            subCategoryId = Optional.present(subCategoryId),
                            title = title,
                            description = description,
                            amount = amount,
                            date = datetime,
                        ),
                    ),
                )
                .execute()
        }.getOrNull()
    }

    public suspend fun get(
        id: ImportedMailId,
    ): Result<ApolloResponse<AddMoneyUsageScreenQuery.Data>> {
        return runCatching {
            apolloClient
                .query(
                    AddMoneyUsageScreenQuery(
                        id = id
                    )
                )
                .execute()
        }
    }
}
