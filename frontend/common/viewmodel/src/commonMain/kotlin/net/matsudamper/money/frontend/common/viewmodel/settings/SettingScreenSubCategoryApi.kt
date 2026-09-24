package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.SubCategorySettingScreenQuery
import net.matsudamper.money.frontend.graphql.UpdateSubCategoryMutation
import net.matsudamper.money.frontend.graphql.type.UpdateSubCategoryQuery

private const val TAG = "SettingScreenSubCategoryApi"

public class SettingScreenSubCategoryApi(
    private val apolloClient: ApolloClient,
) {
    public fun getSubCategoryInfo(id: MoneyUsageSubCategoryId): Flow<ApolloResponse<SubCategorySettingScreenQuery.Data>> {
        return apolloClient
            .query(SubCategorySettingScreenQuery(id = id))
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch()
    }

    public suspend fun updateSubCategory(
        id: MoneyUsageSubCategoryId,
        name: String,
    ): ApolloResponse<UpdateSubCategoryMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    UpdateSubCategoryMutation(
                        id = id,
                        query = UpdateSubCategoryQuery(
                            name = Optional.present(name),
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }
}
