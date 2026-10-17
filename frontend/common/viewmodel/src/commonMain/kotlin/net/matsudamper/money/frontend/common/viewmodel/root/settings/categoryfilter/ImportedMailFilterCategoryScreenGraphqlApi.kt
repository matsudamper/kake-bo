package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailCategoryFilterMatcherId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImportedMailFilterCategoryScreenUiState
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenAddMatcherMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenDeleteFilterMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenDeleteMatcherMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenUpdateMatcherMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterUpdateMutation
import net.matsudamper.money.frontend.graphql.type.AddImportedMailCategoryFilterMatcherInput
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterMatcherType
import net.matsudamper.money.frontend.graphql.type.UpdateImportedMailCategoryFilterInput
import net.matsudamper.money.frontend.graphql.type.UpdateImportedMailCategoryFilterMatcherInput

public class ImportedMailFilterCategoryScreenGraphqlApi(
    private val apolloClient: ApolloClient,
) {
    public suspend fun addMatcher(id: ImportedMailCategoryFilterId): Result<ApolloResponse<ImportedMailCategoryFilterScreenAddMatcherMutation.Data>> {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterScreenAddMatcherMutation(
                        input = AddImportedMailCategoryFilterMatcherInput(
                            id = id,
                        ),
                    ),
                )
                .execute()
        }
    }

    public suspend fun updateFilter(
        id: ImportedMailCategoryFilterId,
        title: String? = null,
        subCategoryId: MoneyUsageSubCategoryId? = null,
        matchExpression: String? = null,
    ): Result<ApolloResponse<ImportedMailCategoryFilterUpdateMutation.Data>> {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterUpdateMutation(
                        UpdateImportedMailCategoryFilterInput(
                            id = id,
                            title = Optional.present(title),
                            subCategoryId = Optional.present(subCategoryId),
                            matchExpression = Optional.presentIfNotNull(matchExpression),
                        ),
                    ),
                ).execute()
        }
    }

    public suspend fun updateMatcher(
        id: ImportedMailCategoryFilterMatcherId,
        matcherKey: String? = null,
        text: String? = null,
        type: ImportedMailFilterCategoryScreenUiState.MatcherType? = null,
        dataSource: ImportedMailFilterCategoryScreenUiState.DataSource? = null,
    ): Result<ApolloResponse<ImportedMailCategoryFilterScreenUpdateMatcherMutation.Data>> {
        return runCatching {
            apolloClient.mutation(
                ImportedMailCategoryFilterScreenUpdateMatcherMutation(
                    input = UpdateImportedMailCategoryFilterMatcherInput(
                        id = id,
                        matcherKey = Optional.present(matcherKey),
                        text = Optional.present(text),
                        matcherType = when (type) {
                            ImportedMailFilterCategoryScreenUiState.MatcherType.Include -> ImportedMailCategoryFilterMatcherType.Include
                            ImportedMailFilterCategoryScreenUiState.MatcherType.NotInclude -> ImportedMailCategoryFilterMatcherType.NotInclude
                            ImportedMailFilterCategoryScreenUiState.MatcherType.Equal -> ImportedMailCategoryFilterMatcherType.Equal
                            ImportedMailFilterCategoryScreenUiState.MatcherType.NotEqual -> ImportedMailCategoryFilterMatcherType.NotEqual
                            ImportedMailFilterCategoryScreenUiState.MatcherType.Unknown,
                            null,
                            -> null
                        }.let { Optional.present(it) },
                        dataSourceType = when (dataSource) {
                            ImportedMailFilterCategoryScreenUiState.DataSource.MailFrom -> ImportedMailCategoryFilterDataSourceType.MailFrom
                            ImportedMailFilterCategoryScreenUiState.DataSource.MailTitle -> ImportedMailCategoryFilterDataSourceType.MailTitle
                            ImportedMailFilterCategoryScreenUiState.DataSource.MailHtml -> ImportedMailCategoryFilterDataSourceType.MailHtml
                            ImportedMailFilterCategoryScreenUiState.DataSource.MailPlain -> ImportedMailCategoryFilterDataSourceType.MailPlain
                            ImportedMailFilterCategoryScreenUiState.DataSource.Title -> ImportedMailCategoryFilterDataSourceType.Title
                            ImportedMailFilterCategoryScreenUiState.DataSource.ServiceName -> ImportedMailCategoryFilterDataSourceType.ServiceName
                            ImportedMailFilterCategoryScreenUiState.DataSource.Unknown,
                            null,
                            -> null
                        }.let { Optional.present(it) },
                    ),
                ),
            ).execute()
        }
    }

    public suspend fun deleteFilter(id: ImportedMailCategoryFilterId): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterScreenDeleteFilterMutation(
                        id = id,
                    ),
                )
                .execute()
        }.map {
            it.data?.userMutation?.deleteImportedMailCategoryFilter == true
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    public suspend fun deleteMatcher(id: ImportedMailCategoryFilterMatcherId): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterScreenDeleteMatcherMutation(
                        id = id,
                    ),
                )
                .execute()
        }.map {
            it.data?.userMutation?.deleteImportedMailCategoryFilterMatcher == true
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }
}
