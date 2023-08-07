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
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImportedMailFilterCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.frontend.graphql.type.ImportedMailFilterCategoryConditionOperator


public class ImportedMailFilterCategoryViewModel(
    private val coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val id: ImportedMailCategoryFilterId,
    private val api: ImportedMailFilterCategoryScreenGraphqlApi,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val apiResponseCollector = ApolloResponseCollector.create(
        apolloClient = apolloClient,
        query = ImportedMailCategoryFilterScreenQuery(id = id),
    )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val categoryViewModel = object {
        private val event: CategorySelectDialogViewModel.Event = object : CategorySelectDialogViewModel.Event {
            override fun selected(result: CategorySelectDialogViewModel.SelectedResult) {
                coroutineScope.launch {
                    api.updateFilter(
                        id = id,
                        subCategoryId = result.subCategoryId,
                    )
                    viewModel.dismissDialog()
                }
            }
        }
        val viewModel = CategorySelectDialogViewModel(
            coroutineScope = coroutineScope,
            event = event,
        )
    }.viewModel

    public val uiStateFlow: StateFlow<ImportedMailFilterCategoryScreenUiState> = MutableStateFlow(
        ImportedMailFilterCategoryScreenUiState(
            textInput = null,
            loadingState = ImportedMailFilterCategoryScreenUiState.LoadingState.Loading,
            categorySelectDialogUiState = null,
            event = object : ImportedMailFilterCategoryScreenUiState.Event {
                override fun onViewInitialized() {
                    coroutineScope.launch {
                        apiResponseCollector.fetch(this)
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

                                createLoadedUiState(
                                    filter = filter,
                                )
                            }
                        }
                    }
                    uiState.copy(
                        loadingState = loadingState,
                        textInput = viewModelState.textInput,
                        categorySelectDialogUiState = viewModelState.categoryDialogUiState,
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
        coroutineScope.launch {
            categoryViewModel.getUiStateFlow().collectLatest { categoryUiState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        categoryDialogUiState = categoryUiState,
                    )
                }
            }
        }
    }

    private fun createLoadedUiState(
        filter: ImportedMailCategoryFilterScreenQuery.ImportedMailCategoryFilter,
    ): ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded {
        return ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded(
            title = filter.importedMailCategoryFilterScreenItem.title,
            category = run category@{
                val subCategory = filter.importedMailCategoryFilterScreenItem.subCategory ?: return@category null
                val category = subCategory.category

                ImportedMailFilterCategoryScreenUiState.Category(
                    category = category.name,
                    subCategory = subCategory.name,
                )
            },
            conditions = filter.importedMailCategoryFilterScreenItem.conditions.orEmpty().map { condition ->
                ImportedMailFilterCategoryScreenUiState.Condition(
                    text = condition.text,
                    source = when (condition.dataSourceType) {
                        ImportedMailCategoryFilterDataSourceType.MailBody -> ImportedMailFilterCategoryScreenUiState.DataSource.MailBody
                        ImportedMailCategoryFilterDataSourceType.MailFrom -> ImportedMailFilterCategoryScreenUiState.DataSource.MailFrom
                        ImportedMailCategoryFilterDataSourceType.MailTitle -> ImportedMailFilterCategoryScreenUiState.DataSource.MailTitle
                        ImportedMailCategoryFilterDataSourceType.ServiceName -> ImportedMailFilterCategoryScreenUiState.DataSource.ServiceName
                        ImportedMailCategoryFilterDataSourceType.Title -> ImportedMailFilterCategoryScreenUiState.DataSource.Title
                        ImportedMailCategoryFilterDataSourceType.UNKNOWN__ -> ImportedMailFilterCategoryScreenUiState.DataSource.Unknown
                    },
                    conditionType = ImportedMailFilterCategoryScreenUiState.ConditionType.NotInclude,
                    event = object : ImportedMailFilterCategoryScreenUiState.ConditionEvent {
                        override fun onClickTextChange() {
                            TODO("Not yet implemented")
                        }

                        override fun selectedSource(source: ImportedMailFilterCategoryScreenUiState.DataSource) {
                            TODO("Not yet implemented")
                        }

                        override fun selectedConditionType(type: ImportedMailFilterCategoryScreenUiState.ConditionType) {
                            TODO("Not yet implemented")
                        }
                    },
                )
            }.toImmutableList(),
            operator = when (filter.importedMailCategoryFilterScreenItem.operator) {
                ImportedMailFilterCategoryConditionOperator.AND -> ImportedMailFilterCategoryScreenUiState.Operator.AND
                ImportedMailFilterCategoryConditionOperator.OR -> ImportedMailFilterCategoryScreenUiState.Operator.OR
                ImportedMailFilterCategoryConditionOperator.UNKNOWN__ -> ImportedMailFilterCategoryScreenUiState.Operator.UNKNOWN
            },
            event = object : ImportedMailFilterCategoryScreenUiState.LoadedEvent {
                override fun onClickAddCondition() {
                    coroutineScope.launch {
                        api.addCondition(id = id)
                            .onFailure {
                                eventSender.send {
                                    it.showNativeAlert("追加に失敗しました。")
                                }
                            }
                    }
                }

                override fun onClickNameChange() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            textInput = ImportedMailFilterCategoryScreenUiState.TextInput(
                                title = "タイトルを変更",
                                onCompleted = {
                                    coroutineScope.launch {
                                        api.updateFilter(id = id, title = it)
                                            .onSuccess {
                                                dismissTextInput()
                                            }
                                            .onFailure {
                                                eventSender.send {
                                                    it.showNativeAlert("更新に失敗しました。")
                                                }
                                            }
                                    }
                                },
                                default = filter.importedMailCategoryFilterScreenItem.title,
                                dismiss = { dismissTextInput() }
                            ),
                        )
                    }
                }

                override fun onSelectedOperator(operator: ImportedMailFilterCategoryScreenUiState.Operator) {
                    // TODO
                }

                override fun onClickCategoryChange() {
                    val subCategory = viewModelStateFlow.value.apolloResponseState.getSuccessOrNull()
                        ?.value?.data?.user?.importedMailCategoryFilter?.importedMailCategoryFilterScreenItem
                        ?.subCategory
                    val category = subCategory?.category
                    categoryViewModel.showDialog(
                        categoryId = category?.id,
                        categoryName = category?.name,
                        subCategoryId = subCategory?.id,
                        subCategoryName = subCategory?.name,
                    )
                }
            },
        )
    }

    private fun dismissTextInput() {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                textInput = null,
            )
        }
    }

    public interface Event {
        public fun showNativeAlert(text: String)
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponseState<ApolloResponse<ImportedMailCategoryFilterScreenQuery.Data>> = ApolloResponseState.loading(),
        val textInput: ImportedMailFilterCategoryScreenUiState.TextInput? = null,
        val categoryDialogUiState: CategorySelectDialogUiState? = null,
    )
}
