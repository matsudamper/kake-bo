package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.ImageUploadClient
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.AddMoneyUsageMutation
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenGetSubCategoryQuery
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenSameDateUsagesQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.AddUsageQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter

private const val TAG = "AddMoneyUsageScreenApi"
private const val SAME_DATE_USAGES_SIZE = 100

public class AddMoneyUsageScreenApi(
    private val graphqlClient: GraphqlClient,
    private val imageUploadClient: ImageUploadClient,
) {
    public suspend fun uploadImage(
        bytes: ByteArray,
        contentType: String?,
    ): ImageUploadClient.UploadResult? {
        return imageUploadClient.upload(
            bytes = bytes,
            contentType = contentType,
        )
    }

    public suspend fun addMoneyUsage(
        title: String,
        description: String,
        amount: Int,
        datetime: LocalDateTime,
        subCategoryId: MoneyUsageSubCategoryId?,
        importedMailId: ImportedMailId?,
        imageIds: List<ImageId>?,
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
                            imageIds = Optional.present(imageIds),
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
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

    public suspend fun getSubCategory(subCategoryId: MoneyUsageSubCategoryId): Result<ApolloResponse<AddMoneyUsageScreenGetSubCategoryQuery.Data>> {
        return runCatching {
            graphqlClient.apolloClient
                .query(
                    AddMoneyUsageScreenGetSubCategoryQuery(
                        subCategoryId = subCategoryId,
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }

    public suspend fun getSameDateUsages(date: LocalDate): Result<ApolloResponse<AddMoneyUsageScreenSameDateUsagesQuery.Data>> {
        val sinceDateTime = LocalDateTime(date, LocalTime(0, 0))
        val untilDateTime = LocalDateTime(date.plus(1, DateTimeUnit.DAY), LocalTime(0, 0))
        return runCatching {
            graphqlClient.apolloClient
                .query(
                    AddMoneyUsageScreenSameDateUsagesQuery(
                        query = MoneyUsagesQuery(
                            size = SAME_DATE_USAGES_SIZE,
                            isAsc = true,
                            filter = Optional.present(
                                MoneyUsagesQueryFilter(
                                    sinceDateTime = Optional.present(sinceDateTime),
                                    untilDateTime = Optional.present(untilDateTime),
                                ),
                            ),
                        ),
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }
}
