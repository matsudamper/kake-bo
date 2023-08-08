package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImportedMailFilterCategoryScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenAddConditionMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenDeleteConditionMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenDeleteFilterMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenUpdateConditionMutation
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterUpdateMutation
import net.matsudamper.money.frontend.graphql.type.AddImportedMailCategoryFilterConditionInput
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.frontend.graphql.type.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.frontend.graphql.type.UpdateImportedMailCategoryFilterConditionInput
import net.matsudamper.money.frontend.graphql.type.UpdateImportedMailCategoryFilterInput

public class ImportedMailFilterCategoryScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun addCondition(
        id: ImportedMailCategoryFilterId,
    ): Result<ApolloResponse<ImportedMailCategoryFilterScreenAddConditionMutation.Data>> {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterScreenAddConditionMutation(
                        input = AddImportedMailCategoryFilterConditionInput(
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
        operator: ImportedMailFilterCategoryScreenUiState.Operator? = null,
    ): Result<ApolloResponse<ImportedMailCategoryFilterUpdateMutation.Data>> {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterUpdateMutation(
                        UpdateImportedMailCategoryFilterInput(
                            id = id,
                            title = Optional.present(title),
                            subCategoryId = Optional.present(subCategoryId),
                            operator = Optional.present(
                                when (operator) {
                                    ImportedMailFilterCategoryScreenUiState.Operator.AND -> ImportedMailFilterCategoryConditionOperator.AND
                                    ImportedMailFilterCategoryScreenUiState.Operator.OR -> ImportedMailFilterCategoryConditionOperator.OR
                                    ImportedMailFilterCategoryScreenUiState.Operator.UNKNOWN -> null
                                    null -> null
                                },
                            ),
                        ),
                    ),
                ).execute()
        }
    }

    public suspend fun updateCondition(
        id: ImportedMailCategoryFilterConditionId,
        text: String? = null,
        type: ImportedMailFilterCategoryScreenUiState.ConditionType? = null,
        dataSource: ImportedMailFilterCategoryScreenUiState.DataSource? = null,
    ): Result<ApolloResponse<ImportedMailCategoryFilterScreenUpdateConditionMutation.Data>> {
        return runCatching {
            apolloClient.mutation(
                ImportedMailCategoryFilterScreenUpdateConditionMutation(
                    input = UpdateImportedMailCategoryFilterConditionInput(
                        id = id,
                        text = Optional.present(text),
                        conditionType = when (type) {
                            ImportedMailFilterCategoryScreenUiState.ConditionType.Include -> ImportedMailCategoryFilterConditionType.Include
                            ImportedMailFilterCategoryScreenUiState.ConditionType.NotInclude -> ImportedMailCategoryFilterConditionType.NotInclude
                            ImportedMailFilterCategoryScreenUiState.ConditionType.Equal -> ImportedMailCategoryFilterConditionType.Equal
                            ImportedMailFilterCategoryScreenUiState.ConditionType.NotEqual -> ImportedMailCategoryFilterConditionType.NotEqual
                            ImportedMailFilterCategoryScreenUiState.ConditionType.Unknown,
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
                    )
                )
                .execute()
        }.map {
            it.data?.userMutation?.deleteImportedMailCategoryFilter == true
        }.fold(
            onSuccess = { it },
            onFailure = { false }
        )
    }

    public suspend fun deleteCondition(id: ImportedMailCategoryFilterConditionId): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    ImportedMailCategoryFilterScreenDeleteConditionMutation(
                        id = id,
                    )
                )
                .execute()
        }.map {
            it.data?.userMutation?.deleteImportedMailCategoryFilterCondition == true
        }.fold(
            onSuccess = { it },
            onFailure = { false }
        )
    }
}
