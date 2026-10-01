package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenSameDateTimeUsagesQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.AddUsageQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter

private const val TAG = "AddMoneyUsageScreenApi"
private const val SAME_DATE_TIME_USAGES_SIZE = 5

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

    public suspend fun getSameDateTimeUsages(datetime: LocalDateTime): Result<ApolloResponse<AddMoneyUsageScreenSameDateTimeUsagesQuery.Data>> {
        val sinceDateTime = LocalDateTime(
            date = datetime.date,
            time = LocalTime(datetime.hour, datetime.minute, datetime.second),
        )
        val untilDateTime = sinceDateTime.toInstant(TimeZone.UTC)
            .plus(1, DateTimeUnit.SECOND, TimeZone.UTC)
            .toLocalDateTime(TimeZone.UTC)
        return runCatching {
            graphqlClient.apolloClient
                .query(
                    AddMoneyUsageScreenSameDateTimeUsagesQuery(
                        query = MoneyUsagesQuery(
                            size = SAME_DATE_TIME_USAGES_SIZE,
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
