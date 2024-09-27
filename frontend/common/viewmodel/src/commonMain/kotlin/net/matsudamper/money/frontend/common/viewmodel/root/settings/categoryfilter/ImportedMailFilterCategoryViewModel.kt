package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.layout.SnackbarEventState
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImportedMailFilterCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.ViewModelFeature
import net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.frontend.graphql.type.ImportedMailFilterCategoryConditionOperator

public class ImportedMailFilterCategoryViewModel(
    viewModelFeature: ViewModelFeature,
    private val graphqlClient: GraphqlClient,
    private val id: ImportedMailCategoryFilterId,
    private val api: ImportedMailFilterCategoryScreenGraphqlApi,
) : CommonViewModel(viewModelFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val apiResponseCollector =
        ApolloResponseCollector.create(
            apolloClient = graphqlClient.apolloClient,
            query = ImportedMailCategoryFilterScreenQuery(id = id),
        )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val categoryViewModel =
        object {
            private val event: CategorySelectDialogViewModel.Event =
                object : CategorySelectDialogViewModel.Event {
                    override fun selected(result: CategorySelectDialogViewModel.SelectedResult) {
                        viewModelScope.launch {
                            api.updateFilter(
                                id = id,
                                subCategoryId = result.subCategoryId,
                            )
                            viewModel.dismissDialog()
                        }
                    }
                }
            val viewModel =
                CategorySelectDialogViewModel(
                    viewModelFeature = viewModelFeature,
                    event = event,
                    apolloClient = graphqlClient.apolloClient,
                )
        }.viewModel

    private val snackbarEventState = SnackbarEventState()
    public val uiStateFlow: StateFlow<ImportedMailFilterCategoryScreenUiState> =
        MutableStateFlow(
            ImportedMailFilterCategoryScreenUiState(
                textInput = null,
                loadingState = ImportedMailFilterCategoryScreenUiState.LoadingState.Loading,
                categorySelectDialogUiState = null,
                snackbarEventState = snackbarEventState,
                confirmDialog = null,
                event =
                object : ImportedMailFilterCategoryScreenUiState.Event {
                    override fun onViewInitialized() {
                        viewModelScope.launch {
                            apiResponseCollector.fetch()
                        }
                    }

                    override fun onClickMenuDelete() {
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                confirmDialog =
                                ImportedMailFilterCategoryScreenUiState.ConfirmDialog(
                                    title = "このフィルタを削除しますか",
                                    description = null,
                                    onConfirm = {
                                        viewModelScope.launch {
                                            val isSuccess = api.deleteFilter(id = id)
                                            dismissConfirmDialog()
                                            if (isSuccess) {
                                                eventSender.send {
                                                    it.navigate(ScreenStructure.Root.Settings.MailCategoryFilters)
                                                }
                                            } else {
                                                snackbarEventState.show(
                                                    SnackbarEventState.Event(
                                                        message = "削除に失敗しました",
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                    onDismiss = { dismissConfirmDialog() },
                                ),
                            )
                        }
                    }

                    override fun onClickBack() {
                        viewModelScope.launch {
                            eventSender.send {
                                it.navigate(ScreenStructure.Root.Settings.MailCategoryFilters)
                            }
                        }
                    }
                },
            ),
        ).also { uiStateFlow ->
            viewModelScope.launch {
                viewModelStateFlow.collectLatest { viewModelState ->
                    uiStateFlow.update { uiState ->
                        val loadingState =
                            run loadingState@{
                                when (val response = viewModelState.apolloResponseState) {
                                    is ApolloResponseState.Failure -> ImportedMailFilterCategoryScreenUiState.LoadingState.Error
                                    is ApolloResponseState.Loading -> ImportedMailFilterCategoryScreenUiState.LoadingState.Loading
                                    is ApolloResponseState.Success -> {
                                        val filter =
                                            response.value.data?.user?.importedMailCategoryFilter
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
                            confirmDialog = viewModelState.confirmDialog,
                        )
                    }
                }
            }
        }.asStateFlow()

    init {
        viewModelScope.launch {
            apiResponseCollector.getFlow().collectLatest { response ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        apolloResponseState = response,
                    )
                }
            }
        }
        viewModelScope.launch {
            categoryViewModel.getUiStateFlow().collectLatest { categoryUiState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        categoryDialogUiState = categoryUiState,
                    )
                }
            }
        }
    }

    private fun createLoadedUiState(filter: ImportedMailCategoryFilterScreenQuery.ImportedMailCategoryFilter): ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded {
        return ImportedMailFilterCategoryScreenUiState.LoadingState.Loaded(
            title = filter.importedMailCategoryFilterScreenItem.title,
            category =
            run category@{
                val subCategory = filter.importedMailCategoryFilterScreenItem.subCategory ?: return@category null
                val category = subCategory.category

                ImportedMailFilterCategoryScreenUiState.Category(
                    category = category.name,
                    subCategory = subCategory.name,
                )
            },
            conditions =
            filter.importedMailCategoryFilterScreenItem.conditions.orEmpty()
                .map { it.importedMailCategoryConditionScreenItem }
                .map { condition ->
                    ImportedMailFilterCategoryScreenUiState.Condition(
                        text = condition.text,
                        source =
                        when (condition.dataSourceType) {
                            ImportedMailCategoryFilterDataSourceType.MailHtml -> ImportedMailFilterCategoryScreenUiState.DataSource.MailHtml
                            ImportedMailCategoryFilterDataSourceType.MailPlain -> ImportedMailFilterCategoryScreenUiState.DataSource.MailPlain
                            ImportedMailCategoryFilterDataSourceType.MailFrom -> ImportedMailFilterCategoryScreenUiState.DataSource.MailFrom
                            ImportedMailCategoryFilterDataSourceType.MailTitle -> ImportedMailFilterCategoryScreenUiState.DataSource.MailTitle
                            ImportedMailCategoryFilterDataSourceType.ServiceName -> ImportedMailFilterCategoryScreenUiState.DataSource.ServiceName
                            ImportedMailCategoryFilterDataSourceType.Title -> ImportedMailFilterCategoryScreenUiState.DataSource.Title
                            ImportedMailCategoryFilterDataSourceType.UNKNOWN__ -> ImportedMailFilterCategoryScreenUiState.DataSource.Unknown
                        },
                        conditionType =
                        when (condition.conditionType) {
                            ImportedMailCategoryFilterConditionType.Equal -> ImportedMailFilterCategoryScreenUiState.ConditionType.Equal
                            ImportedMailCategoryFilterConditionType.Include -> ImportedMailFilterCategoryScreenUiState.ConditionType.Include
                            ImportedMailCategoryFilterConditionType.NotEqual -> ImportedMailFilterCategoryScreenUiState.ConditionType.NotEqual
                            ImportedMailCategoryFilterConditionType.NotInclude -> ImportedMailFilterCategoryScreenUiState.ConditionType.NotInclude
                            ImportedMailCategoryFilterConditionType.UNKNOWN__ -> ImportedMailFilterCategoryScreenUiState.ConditionType.Unknown
                        },
                        event =
                        object : ImportedMailFilterCategoryScreenUiState.ConditionEvent {
                            override fun onClickTextChange() {
                                viewModelStateFlow.update { viewModelState ->
                                    viewModelState.copy(
                                        textInput =
                                        ImportedMailFilterCategoryScreenUiState.TextInput(
                                            title = "条件のテキストを編集",
                                            onCompleted = { text ->
                                                viewModelScope.launch {
                                                    api.updateCondition(
                                                        id = condition.id,
                                                        text = text,
                                                    ).onFailure {
                                                        eventSender.send {
                                                            it.showNativeAlert("更新に失敗しました")
                                                        }
                                                    }.onSuccess {
                                                        dismissTextInput()
                                                    }
                                                }
                                            },
                                            default = condition.text,
                                            dismiss = { dismissTextInput() },
                                        ),
                                    )
                                }
                            }

                            override fun selectedSource(source: ImportedMailFilterCategoryScreenUiState.DataSource) {
                                viewModelScope.launch {
                                    api.updateCondition(
                                        id = condition.id,
                                        dataSource = source,
                                    ).onFailure {
                                        eventSender.send {
                                            it.showNativeAlert("更新に失敗しました")
                                        }
                                    }
                                }
                            }

                            override fun selectedConditionType(type: ImportedMailFilterCategoryScreenUiState.ConditionType) {
                                viewModelScope.launch {
                                    api.updateCondition(
                                        id = condition.id,
                                        type = type,
                                    ).onFailure {
                                        eventSender.send {
                                            it.showNativeAlert("更新に失敗しました")
                                        }
                                    }
                                }
                            }

                            override fun onClickDeleteMenu() {
                                viewModelStateFlow.update { viewModelState ->
                                    viewModelState.copy(
                                        confirmDialog =
                                        ImportedMailFilterCategoryScreenUiState.ConfirmDialog(
                                            title = "この条件を削除しますか？",
                                            description = null,
                                            onDismiss = {
                                                dismissConfirmDialog()
                                            },
                                            onConfirm = {
                                                viewModelScope.launch {
                                                    val isSuccess = api.deleteCondition(id = condition.id)
                                                    dismissConfirmDialog()
                                                    if (isSuccess) {
                                                        launch {
                                                            snackbarEventState.show(
                                                                SnackbarEventState.Event(
                                                                    message = "削除しました",
                                                                ),
                                                            )
                                                        }
                                                        apiResponseCollector.fetch()
                                                    } else {
                                                        snackbarEventState.show(
                                                            SnackbarEventState.Event(
                                                                message = "削除に失敗しました",
                                                            ),
                                                        )
                                                    }
                                                }
                                            },
                                        ),
                                    )
                                }
                            }
                        },
                    )
                }.toImmutableList(),
            operator =
            when (filter.importedMailCategoryFilterScreenItem.operator) {
                ImportedMailFilterCategoryConditionOperator.AND -> ImportedMailFilterCategoryScreenUiState.Operator.AND
                ImportedMailFilterCategoryConditionOperator.OR -> ImportedMailFilterCategoryScreenUiState.Operator.OR
                ImportedMailFilterCategoryConditionOperator.UNKNOWN__ -> ImportedMailFilterCategoryScreenUiState.Operator.UNKNOWN
            },
            event =
            object : ImportedMailFilterCategoryScreenUiState.LoadedEvent {
                override fun onClickAddCondition() {
                    viewModelScope.launch {
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
                            textInput =
                            ImportedMailFilterCategoryScreenUiState.TextInput(
                                title = "タイトルを変更",
                                onCompleted = {
                                    viewModelScope.launch {
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
                                dismiss = { dismissTextInput() },
                            ),
                        )
                    }
                }

                override fun onSelectedOperator(operator: ImportedMailFilterCategoryScreenUiState.Operator) {
                    viewModelScope.launch {
                        runCatching {
                            api.updateFilter(
                                id = id,
                                operator = operator,
                            )
                        }.onFailure {
                            snackbarEventState.show(
                                SnackbarEventState.Event(
                                    message = "更新に失敗しました",
                                    withDismissAction = true,
                                ),
                            )
                        }
                    }
                }

                override fun onClickCategoryChange() {
                    val subCategory =
                        viewModelStateFlow.value.apolloResponseState.getSuccessOrNull()
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

    private fun dismissConfirmDialog() {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                confirmDialog = null,
            )
        }
    }

    public interface Event {
        public fun showNativeAlert(text: String)

        public fun navigate(structure: ScreenStructure)
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponseState<ApolloResponse<ImportedMailCategoryFilterScreenQuery.Data>> = ApolloResponseState.loading(),
        val textInput: ImportedMailFilterCategoryScreenUiState.TextInput? = null,
        val categoryDialogUiState: CategorySelectDialogUiState? = null,
        val confirmDialog: ImportedMailFilterCategoryScreenUiState.ConfirmDialog? = null,
    )
}
