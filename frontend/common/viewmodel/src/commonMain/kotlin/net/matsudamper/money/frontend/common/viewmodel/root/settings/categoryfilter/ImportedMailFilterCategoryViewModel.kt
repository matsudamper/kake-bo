package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImportedMailFilterCategoryScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class ImportedMailFilterCategoryViewModel(
    private val coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val id: ImportedMailCategoryFilterId,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val apiResponseCollector = ApolloResponseCollector.create(
        apolloClient = apolloClient,
        query = ImportedMailCategoryFilterScreenQuery(id = id),
    )
    public val uiStateFlow: StateFlow<ImportedMailFilterCategoryScreenUiState> = MutableStateFlow(
        ImportedMailFilterCategoryScreenUiState(
            textInput = null,
            loadingState = ImportedMailFilterCategoryScreenUiState.LoadingState.Loading,
            event = object : ImportedMailFilterCategoryScreenUiState.Event {
                override fun onViewInitialized() {
                    coroutineScope.launch {
                        apiResponseCollector.fetch()
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    val loadingState = run loadingState@{
                        when (val response = viewModelState.apolloResponseState) {
                            is ApolloResponseState.Failure -> ImportedMailFilterCategoryScreenUiState.LoadingState.Error
                            is ApolloResponseState.Loading -> ImportedMailFilterCategoryScreenUiState.LoadingState.Loading
                            is ApolloResponseState.Success -> {
                                val filter = response.value.data?.user?.importedMailCategoryFilter
                                    ?: return@loadingState ImportedMailFilterCategoryScreenUiState.LoadingState.Error

                                ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded(
                                    title = filter.title,
                                    category = run category@{
                                        val subCategory = filter.subCategory ?: return@category null
                                        val category = subCategory.category

                                        ImportedMailFilterCategoryScreenUiState.Category(
                                            category = category.name,
                                            subCategory = subCategory.name,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    uiState.copy(
                        loadingState = loadingState,
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            apiResponseCollector.flow.collectLatest { response ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        apolloResponseState = response,
                    )
                }
            }
        }
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponseState<ApolloResponse<ImportedMailCategoryFilterScreenQuery.Data>> = ApolloResponseState.loading(),
    )
}