package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsagePresetId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.AddMoneyUsagePresetMutation
import net.matsudamper.money.frontend.graphql.DeleteMoneyUsagePresetMutation
import net.matsudamper.money.frontend.graphql.GetMoneyUsagePresetQuery
import net.matsudamper.money.frontend.graphql.GetMoneyUsagePresetsQuery
import net.matsudamper.money.frontend.graphql.UpdateMoneyUsagePresetMutation
import net.matsudamper.money.frontend.graphql.type.AddMoneyUsagePresetInput
import net.matsudamper.money.frontend.graphql.type.UpdateMoneyUsagePresetInput

public class PresetScreenApi(
    private val apolloClient: ApolloClient,
) {
    public fun getPresets(): Flow<ApolloResponse<GetMoneyUsagePresetsQuery.Data>> {
        return apolloClient
            .query(GetMoneyUsagePresetsQuery())
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch()
    }

    public suspend fun getPreset(id: MoneyUsagePresetId): ApolloResponse<GetMoneyUsagePresetQuery.Data>? {
        return runCatching {
            apolloClient
                .query(GetMoneyUsagePresetQuery(id = id))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    public suspend fun addPreset(
        name: String,
        subCategoryId: MoneyUsageSubCategoryId?,
        amount: Int?,
        description: String?,
    ): ApolloResponse<AddMoneyUsagePresetMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    AddMoneyUsagePresetMutation(
                        input = AddMoneyUsagePresetInput(
                            name = name,
                            subCategoryId = Optional.present(subCategoryId),
                            amount = Optional.present(amount),
                            description = Optional.present(description),
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    public suspend fun deletePreset(id: MoneyUsagePresetId): Boolean {
        return runCatching {
            apolloClient
                .mutation(DeleteMoneyUsagePresetMutation(id = id))
                .execute()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()?.data?.userMutation?.deleteMoneyUsagePreset == true
    }

    public suspend fun updatePreset(
        id: MoneyUsagePresetId,
        name: Optional<String?> = Optional.Absent,
        subCategoryId: Optional<MoneyUsageSubCategoryId?> = Optional.Absent,
        amount: Optional<Int?> = Optional.Absent,
        description: Optional<String?> = Optional.Absent,
    ): ApolloResponse<UpdateMoneyUsagePresetMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    UpdateMoneyUsagePresetMutation(
                        input = UpdateMoneyUsagePresetInput(
                            id = id,
                            name = name,
                            subCategoryId = subCategoryId,
                            amount = amount,
                            description = description,
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }
}
