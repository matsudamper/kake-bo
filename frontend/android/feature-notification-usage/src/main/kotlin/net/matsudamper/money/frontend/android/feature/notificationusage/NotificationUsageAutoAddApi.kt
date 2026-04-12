package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo.api.Optional
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.runCatchingWithoutCancel
import net.matsudamper.money.frontend.graphql.AddMoneyUsageMutation
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.AddUsageQuery

internal data class NotificationUsageAutoAddPayload(
    val title: String,
    val description: String,
    val amount: Int,
    val dateTime: LocalDateTime,
    val subCategoryId: MoneyUsageSubCategoryId?,
)

internal interface NotificationUsageAutoAddApi {
    suspend fun addUsage(payload: NotificationUsageAutoAddPayload): MoneyUsageId?
}

internal class NotificationUsageAutoAddGraphqlApi(
    private val graphqlClient: GraphqlClient,
) : NotificationUsageAutoAddApi {
    override suspend fun addUsage(payload: NotificationUsageAutoAddPayload): MoneyUsageId? {
        return runCatchingWithoutCancel {
            graphqlClient.apolloClient
                .mutation(
                    AddMoneyUsageMutation(
                        query = AddUsageQuery(
                            title = payload.title,
                            description = payload.description,
                            subCategoryId = Optional.present(payload.subCategoryId),
                            amount = payload.amount,
                            date = payload.dateTime,
                            importedMailId = Optional.absent(),
                            imageIds = Optional.absent(),
                        ),
                    ),
                )
                .execute()
                .data?.userMutation?.addUsage?.id
        }.getOrNull()
    }
}
