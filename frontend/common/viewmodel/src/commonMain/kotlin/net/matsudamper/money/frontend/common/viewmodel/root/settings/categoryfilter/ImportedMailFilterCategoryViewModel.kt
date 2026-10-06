package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.ApolloResponse
import net.matsudamper.money.categoryfilter.CategoryFilterMatcherKey
import net.matsudamper.money.categoryfilter.matchexpression.MatchExpressionAnalysis
import net.matsudamper.money.categoryfilter.matchexpression.MatchExpressionAnalyzer
import net.matsudamper.money.categoryfilter.matchexpression.MatchExpressionError
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailCategoryFilterMatcherId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.SnackbarEventState
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImportedMailFilterCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters.ImportedMailCategoryFilterScreenPagingModel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFilterScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterMatcherType
import net.matsudamper.money.frontend.graphql.type.ImportedMailFilterCategoryConditionOperator

public class ImportedMailFilterCategoryViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
    private val id: ImportedMailCategoryFilterId,
    private val api: ImportedMailFilterCategoryScreenGraphqlApi,
    private val pagingModel: ImportedMailCategoryFilterScreenPagingModel,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val apiResponseCollector = ApolloResponseCollector.create(
        apolloClient = graphqlClient.apolloClient,
        query = ImportedMailCategoryFilterScreenQuery(id = id),
    )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val categoryViewModel = object {
        private val event: CategorySelectDialogViewModel.Event = object : CategorySelectDialogViewModel.Event {
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
        val viewModel = CategorySelectDialogViewModel(
            scopedObjectFeature = scopedObjectFeature,
            event = event,
            apolloClient = graphqlClient.apolloClient,
        )
    }.viewModel

    private val snackbarEventState = SnackbarEventState()
    public val uiStateFlow: StateFlow<ImportedMailFilterCategoryScreenUiState> = MutableStateFlow(
        ImportedMailFilterCategoryScreenUiState(
            textInput = null,
            loadingState = ImportedMailFilterCategoryScreenUiState.LoadingState.Loading,
            categorySelectDialogUiState = null,
            snackbarEventState = snackbarEventState,
            confirmDialog = null,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            event = object : ImportedMailFilterCategoryScreenUiState.Event {
                override fun onViewInitialized() {
                    viewModelScope.launch {
                        apiResponseCollector.fetch()
                    }
                }

                override fun onClickMenuDelete() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            confirmDialog = ImportedMailFilterCategoryScreenUiState.ConfirmDialog(
                                title = "このフィルタを削除しますか",
                                description = null,
                                onConfirm = {
                                    viewModelScope.launch {
                                        val isSuccess = api.deleteFilter(id = id)
                                        dismissConfirmDialog()
                                        if (isSuccess) {
                                            pagingModel.removeFilterFromCache(id)
                                            eventSender.send {
                                                it.navigateBack()
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
                            it.navigateBack()
                        }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
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
        val matchers = filter.importedMailCategoryFilterScreenItem.matchers.orEmpty()
            .map { it.importedMailCategoryFilterMatcherScreenItem }
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
            matchers = matchers
                .map { matcher ->
                    ImportedMailFilterCategoryScreenUiState.Matcher(
                        matcherKey = matcher.matcherKey,
                        text = matcher.text,
                        source = when (matcher.dataSourceType) {
                            ImportedMailCategoryFilterDataSourceType.MailHtml -> ImportedMailFilterCategoryScreenUiState.DataSource.MailHtml
                            ImportedMailCategoryFilterDataSourceType.MailPlain -> ImportedMailFilterCategoryScreenUiState.DataSource.MailPlain
                            ImportedMailCategoryFilterDataSourceType.MailFrom -> ImportedMailFilterCategoryScreenUiState.DataSource.MailFrom
                            ImportedMailCategoryFilterDataSourceType.MailTitle -> ImportedMailFilterCategoryScreenUiState.DataSource.MailTitle
                            ImportedMailCategoryFilterDataSourceType.ServiceName -> ImportedMailFilterCategoryScreenUiState.DataSource.ServiceName
                            ImportedMailCategoryFilterDataSourceType.Title -> ImportedMailFilterCategoryScreenUiState.DataSource.Title
                            ImportedMailCategoryFilterDataSourceType.UNKNOWN__ -> ImportedMailFilterCategoryScreenUiState.DataSource.Unknown
                        },
                        matcherType = when (matcher.matcherType) {
                            ImportedMailCategoryFilterMatcherType.Equal -> ImportedMailFilterCategoryScreenUiState.MatcherType.Equal
                            ImportedMailCategoryFilterMatcherType.Include -> ImportedMailFilterCategoryScreenUiState.MatcherType.Include
                            ImportedMailCategoryFilterMatcherType.NotEqual -> ImportedMailFilterCategoryScreenUiState.MatcherType.NotEqual
                            ImportedMailCategoryFilterMatcherType.NotInclude -> ImportedMailFilterCategoryScreenUiState.MatcherType.NotInclude
                            ImportedMailCategoryFilterMatcherType.UNKNOWN__ -> ImportedMailFilterCategoryScreenUiState.MatcherType.Unknown
                        },
                        event = object : ImportedMailFilterCategoryScreenUiState.MatcherEvent {
                            override fun onClickMatcherKeyChange() {
                                viewModelStateFlow.update { viewModelState ->
                                    viewModelState.copy(
                                        textInput = ImportedMailFilterCategoryScreenUiState.TextInput(
                                            title = "キーを編集（${CategoryFilterMatcherKey.ALLOWED_CHARACTERS_DESCRIPTION}）",
                                            onCompleted = { matcherKey ->
                                                viewModelScope.launch {
                                                    updateMatcherKey(
                                                        matcherId = matcher.id,
                                                        matcherKey = matcherKey,
                                                    )
                                                }
                                            },
                                            default = matcher.matcherKey,
                                            isMultiline = false,
                                            dismiss = { dismissTextInput() },
                                        ),
                                    )
                                }
                            }

                            override fun onClickTextChange() {
                                viewModelStateFlow.update { viewModelState ->
                                    viewModelState.copy(
                                        textInput = ImportedMailFilterCategoryScreenUiState.TextInput(
                                            title = "条件のテキストを編集",
                                            onCompleted = { text ->
                                                viewModelScope.launch {
                                                    api.updateMatcher(
                                                        id = matcher.id,
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
                                            default = matcher.text,
                                            isMultiline = false,
                                            dismiss = { dismissTextInput() },
                                        ),
                                    )
                                }
                            }

                            override fun selectedSource(source: ImportedMailFilterCategoryScreenUiState.DataSource) {
                                viewModelScope.launch {
                                    api.updateMatcher(
                                        id = matcher.id,
                                        dataSource = source,
                                    ).onFailure {
                                        eventSender.send {
                                            it.showNativeAlert("更新に失敗しました")
                                        }
                                    }
                                }
                            }

                            override fun selectedMatcherType(type: ImportedMailFilterCategoryScreenUiState.MatcherType) {
                                viewModelScope.launch {
                                    api.updateMatcher(
                                        id = matcher.id,
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
                                        confirmDialog = ImportedMailFilterCategoryScreenUiState.ConfirmDialog(
                                            title = "この条件を削除しますか？",
                                            description = null,
                                            onDismiss = {
                                                dismissConfirmDialog()
                                            },
                                            onConfirm = {
                                                viewModelScope.launch {
                                                    val isSuccess = api.deleteMatcher(id = matcher.id)
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
            matchExpression = createMatchExpressionUiState(
                matchExpression = filter.importedMailCategoryFilterScreenItem.matchExpression,
                matcherKeys = matchers.map { it.matcherKey }.toSet(),
            ),
            operator = when (filter.importedMailCategoryFilterScreenItem.operator) {
                ImportedMailFilterCategoryConditionOperator.AND -> ImportedMailFilterCategoryScreenUiState.Operator.AND
                ImportedMailFilterCategoryConditionOperator.OR -> ImportedMailFilterCategoryScreenUiState.Operator.OR
                ImportedMailFilterCategoryConditionOperator.UNKNOWN__ -> ImportedMailFilterCategoryScreenUiState.Operator.UNKNOWN
            },
            event = object : ImportedMailFilterCategoryScreenUiState.LoadedEvent {
                override fun onClickAddMatcher() {
                    viewModelScope.launch {
                        api.addMatcher(id = id)
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
                                isMultiline = false,
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

                override fun onClickMatchExpressionChange() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            textInput = ImportedMailFilterCategoryScreenUiState.TextInput(
                                title = "式を編集",
                                onCompleted = { matchExpression ->
                                    viewModelScope.launch {
                                        api.updateFilter(
                                            id = id,
                                            matchExpression = matchExpression,
                                        ).onSuccess {
                                            dismissTextInput()
                                        }.onFailure {
                                            eventSender.send {
                                                it.showNativeAlert("更新に失敗しました。")
                                            }
                                        }
                                    }
                                },
                                default = filter.importedMailCategoryFilterScreenItem.matchExpression.orEmpty(),
                                isMultiline = true,
                                dismiss = { dismissTextInput() },
                            ),
                        )
                    }
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

    private suspend fun updateMatcherKey(
        matcherId: ImportedMailCategoryFilterMatcherId,
        matcherKey: String,
    ) {
        if (!CategoryFilterMatcherKey.isValid(matcherKey)) {
            eventSender.send {
                it.showNativeAlert("キーには${CategoryFilterMatcherKey.ALLOWED_CHARACTERS_DESCRIPTION}を${CategoryFilterMatcherKey.MAX_LENGTH}文字以内で入力してください")
            }
            return
        }
        val isSuccess = api.updateMatcher(
            id = matcherId,
            matcherKey = matcherKey,
        ).getOrNull()?.data?.userMutation?.updateImportedMailCategoryFilterMatcher != null
        if (isSuccess) {
            dismissTextInput()
        } else {
            eventSender.send {
                it.showNativeAlert("更新に失敗しました。同じキーが既に使われている可能性があります")
            }
        }
    }

    private fun createMatchExpressionUiState(
        matchExpression: String?,
        matcherKeys: Set<String>,
    ): ImportedMailFilterCategoryScreenUiState.MatchExpression {
        val errors: List<MatchExpressionError> = if (matchExpression.isNullOrBlank()) {
            listOf()
        } else {
            when (val analysis = MatchExpressionAnalyzer.analyze(matchExpression, matcherKeys)) {
                is MatchExpressionAnalysis.Valid -> listOf()
                is MatchExpressionAnalysis.Invalid -> analysis.errors
            }
        }
        return ImportedMailFilterCategoryScreenUiState.MatchExpression(
            text = matchExpression?.takeIf { it.isNotBlank() },
            errorRanges = errors.map { it.range }.toImmutableList(),
            errorMessages = errors.map { createMatchExpressionErrorMessage(it) }.distinct().toImmutableList(),
            allowedCharactersDescription = "キー（${CategoryFilterMatcherKey.ALLOWED_CHARACTERS_DESCRIPTION}）、AND、OR、!、(、)、TRUE、FALSE、空白、改行",
        )
    }

    private fun createMatchExpressionErrorMessage(error: MatchExpressionError): String {
        return when (error.type) {
            MatchExpressionError.Type.Empty -> "式が空です"
            MatchExpressionError.Type.InvalidCharacter -> "使えない文字があります: ${error.text}"
            MatchExpressionError.Type.UnknownKeyword -> "知らないキーワードです: ${error.text}"
            MatchExpressionError.Type.UnexpectedToken -> "ここに書けない語があります: ${error.text}"
            MatchExpressionError.Type.MissingOperand -> "${error.text} の後に条件がありません"
            MatchExpressionError.Type.MissingClosingParenthesis -> "閉じ括弧がありません"
            MatchExpressionError.Type.UnknownMatcherKey -> "存在しないキーです: ${error.text}"
        }
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

        public fun navigateBack()
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponseState<ApolloResponse<ImportedMailCategoryFilterScreenQuery.Data>> = ApolloResponseState.loading(),
        val textInput: ImportedMailFilterCategoryScreenUiState.TextInput? = null,
        val categoryDialogUiState: CategorySelectDialogUiState? = null,
        val confirmDialog: ImportedMailFilterCategoryScreenUiState.ConfirmDialog? = null,
    )
}
