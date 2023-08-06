package net.matsudamper.money.frontend.common.viewmodel.root.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingMailCategoryFilterScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery

public class ImportedMailCategoryFilterScreenPagingModel(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val coroutineScope: CoroutineScope,
) {
    private val pagingState = ApolloPagingResponseCollector.createAndAdd(
        apolloClient = apolloClient,
        coroutineScope = coroutineScope,
        query = ImportedMailCategoryFiltersScreenPagingQuery(
            query = ImportedMailCategoryFiltersQuery(
                cursor = Optional.present(null),
                isAsc = true,
            ),
        ),
    )

    internal val flow = pagingState.flow

    internal fun fetch() {
        val last = pagingState.flow.value.lastOrNull()

        val cursor: String?
        when (last) {
            is ApolloResponseState.Loading -> {
                return
            }

            is ApolloResponseState.Failure -> {
                pagingState.lastRetry()
                return
            }

            is ApolloResponseState.Success -> {
                val response = last.value.data?.user?.importedMailCategoryFilters
                if (response == null) {
                    pagingState.lastRetry()
                    return
                }
                if (response.isLast) {
                    return
                }

                cursor = response.cursor
            }

            null -> {
                cursor = null
            }
        }

        pagingState.add(
            query = ImportedMailCategoryFiltersScreenPagingQuery(
                query = ImportedMailCategoryFiltersQuery(
                    cursor = Optional.present(cursor),
                    isAsc = true,
                ),
            ),
        )
    }
}
