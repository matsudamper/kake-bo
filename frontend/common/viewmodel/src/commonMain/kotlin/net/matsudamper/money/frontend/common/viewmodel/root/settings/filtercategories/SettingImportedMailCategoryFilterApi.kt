package net.matsudamper.money.frontend.common.viewmodel.root.settings.filtercategories

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenAddImportedMailCategoryMutation
import net.matsudamper.money.frontend.graphql.type.AddImportedMailCategoryFilterInput

public class SettingImportedMailCategoryFilterApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun addFilter(title: String): Result<ApolloResponse<ImportedMailCategoryFiltersScreenAddImportedMailCategoryMutation.Data>> {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFiltersScreenAddImportedMailCategoryMutation(
                        input = AddImportedMailCategoryFilterInput(title),
                    ),
                )
                .execute()
        }
    }
}
