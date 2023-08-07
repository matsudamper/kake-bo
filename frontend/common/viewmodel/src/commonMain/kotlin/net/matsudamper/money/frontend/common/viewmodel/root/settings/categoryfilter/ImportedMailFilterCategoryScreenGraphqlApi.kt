package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenAddConditionMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterUpdateMutation
import net.matsudamper.money.frontend.graphql.type.AddImportedMailCategoryFilterConditionInput
import net.matsudamper.money.frontend.graphql.type.UpdateImportedMailCategoryFilterInput

public class ImportedMailFilterCategoryScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun addCondition(
        id: ImportedMailCategoryFilterId
    ): Result<ApolloResponse<ImportedMailCategoryFilterScreenAddConditionMutation.Data>> {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterScreenAddConditionMutation(
                        input = AddImportedMailCategoryFilterConditionInput(
                            id = id,
                        )
                    )
                )
                .execute()
        }
    }

    public suspend fun updateFilter(
        id: ImportedMailCategoryFilterId,
        title: String? = null,
        subCategoryId: MoneyUsageSubCategoryId? = null,
    ): Result<ApolloResponse<ImportedMailCategoryFilterUpdateMutation.Data>> {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterUpdateMutation(
                        UpdateImportedMailCategoryFilterInput(
                            id = id,
                            title = Optional.present(title),
                            subCategoryId = Optional.present(subCategoryId),
                        )
                    )
                ).execute()
        }
    }
}
