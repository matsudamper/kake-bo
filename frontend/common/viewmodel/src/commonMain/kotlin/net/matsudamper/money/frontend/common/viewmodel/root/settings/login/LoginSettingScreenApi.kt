package net.matsudamper.money.frontend.common.viewmodel.root.settings.login

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.graphql.AddCategoryMutation
import net.matsudamper.money.frontend.graphql.CategoriesSettingScreenCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.SettingScreenAddFidoMutation
import net.matsudamper.money.frontend.graphql.type.AddCategoryInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.RegisterFidoInput

public class LoginSettingScreenApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun addFido(
        base64AttestationObject: String,
        base64ClientDataJson: String,
    ): ApolloResponse<SettingScreenAddFidoMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    SettingScreenAddFidoMutation(
                        RegisterFidoInput(
                            base64AttestationObject = base64AttestationObject,
                            base64ClientDataJson = base64ClientDataJson,
                        ),
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }
}