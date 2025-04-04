package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenAddImportedMailCategoryMutation
import net.matsudamper.money.frontend.graphql.type.AddImportedMailCategoryFilterInput

public class SettingImportedMailCategoryFilterApi(
    private val apolloClient: ApolloClient,
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
