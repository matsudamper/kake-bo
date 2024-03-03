package net.matsudamper.money.frontend.common.viewmodel.moneyusage

import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenDeleteUsageMutation
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenUpdateUsageMutation
import net.matsudamper.money.frontend.graphql.type.UpdateUsageQuery

public class MoneyUsageScreenViewModelApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun updateUsage(
        id: MoneyUsageId,
        title: String? = null,
        date: LocalDateTime? = null,
        amount: Int? = null,
        description: String? = null,
        subCategoryId: net.matsudamper.money.element.MoneyUsageSubCategoryId? = null,
    ): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    MoneyUsageScreenUpdateUsageMutation(
                        query =
                            UpdateUsageQuery(
                                id = id,
                                title = Optional.present(title),
                                description = Optional.present(description),
                                amount = Optional.present(amount),
                                date = Optional.present(date),
                                subCategoryId = Optional.present(subCategoryId),
                            ),
                    ),
                )
                .execute()
        }.map {
            it.data?.userMutation?.updateUsage?.moneyUsageScreenMoneyUsage
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it != null },
            onFailure = { false },
        )
    }

    public suspend fun deleteUsage(id: MoneyUsageId): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    MoneyUsageScreenDeleteUsageMutation(
                        id = id,
                    ),
                )
                .execute()
        }.map {
            it.data?.userMutation?.deleteUsage
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it == true },
            onFailure = { false },
        )
    }
}
