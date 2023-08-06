package net.matsudamper.money.frontend.common.viewmodel.layout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageSelectDialogCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.MoneyUsageSelectDialogSubCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryQuery

internal class CategorySelectDialogViewModel(
    private val coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val event: Event,
) {
    private val innerViewModelStateFlow = MutableStateFlow(InnerViewModelState())
    val viewModelStateFlow = innerViewModelStateFlow.map {
        ViewModelState(
            usageCategorySet = it.usageCategorySet,
        )
    }

    private val apolloResponseCollector = ApolloResponseCollector.create(
        apolloClient = apolloClient,
        query = MoneyUsageSelectDialogCategoriesPagingQuery(
            MoneyUsageCategoriesInput(
                size = 100,
                cursor = Optional.present(null),
            ),
        ),
    )
    private val subCategoriesFlow: MutableStateFlow<Map<MoneyUsageCategoryId, ApolloResponseCollector<MoneyUsageSelectDialogSubCategoriesPagingQuery.Data>>> = MutableStateFlow(mapOf())

    suspend fun getUiStateFlow(): StateFlow<CategorySelectDialogUiState?> {
        return channelFlow {
            innerViewModelStateFlow.collectLatest { viewModelState ->
                send(
                    if (viewModelState.categorySelectDialog != null) {
                        createCategorySelectDialogUiState(
                            categoriesApolloResponse = viewModelState.categories,
                            categoryDialogInnerViewModelState = viewModelState.categorySelectDialog,
                            subCategoryApolloResponses = viewModelState.subCategories,
                        )
                    } else {
                        null
                    },
                )
            }
        }.stateIn(coroutineScope, SharingStarted.Lazily, null)
    }

    init {
        coroutineScope.launch {
            apolloResponseCollector.flow.collectLatest { apolloResponseState ->
                innerViewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        categories = apolloResponseState,
                    )
                }
            }
        }
        coroutineScope.launch {
            subCategoriesFlow.collectLatest { map ->
                combine(map.toList().map { pair -> pair.second.flow.map { pair.first to it } }) {
                    it
                }.collectLatest {

                    innerViewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            subCategories = it.toMap(),
                        )
                    }
                }
            }
        }
    }

    fun showDialog() {
        innerViewModelStateFlow.update {
            it.copy(
                categorySelectDialog = InnerViewModelState.CategorySelectDialog(
                    categorySet = it.usageCategorySet,
                    screenType = InnerViewModelState.CategorySelectDialog.ScreenType.Root,
                ),
            )
        }
    }

    // TODO ページング
    // TODO error handling
    fun fetchCategory(useCache: Boolean = true) {
        coroutineScope.launch {
            val requireFetch = if (useCache) {
                true
            } else {
                when (val category = innerViewModelStateFlow.value.categories) {
                    is ApolloResponseState.Failure -> true
                    is ApolloResponseState.Loading -> true
                    is ApolloResponseState.Success -> {
                        category.value.data?.user?.moneyUsageCategories == null
                    }
                }
            }
            if (requireFetch) {
                apolloResponseCollector.fetch(this)
            }
        }
    }

    // TODO ページング
    // TODO error handling
    public fun fetchSubCategories(id: MoneyUsageCategoryId) {
        val beforeItem = subCategoriesFlow.value[id]
        if (beforeItem != null) {
            coroutineScope.launch {
                beforeItem.fetch(this)
            }
            return
        }

        val collector = ApolloResponseCollector.create(
            apolloClient = apolloClient,
            query = MoneyUsageSelectDialogSubCategoriesPagingQuery(
                categoryId = id,
                query = MoneyUsageSubCategoryQuery(
                    size = 100,
                    cursor = Optional.present(null),
                ),
            ),
        )

        subCategoriesFlow.update { subCategories ->
            subCategories.plus(id to collector)
        }

        coroutineScope.launch {
            collector.fetch(this)
        }
    }

    private fun createCategorySelectDialogUiState(
        categoriesApolloResponse: ApolloResponseState<ApolloResponse<MoneyUsageSelectDialogCategoriesPagingQuery.Data>>,
        subCategoryApolloResponses: Map<MoneyUsageCategoryId, ApolloResponseState<ApolloResponse<MoneyUsageSelectDialogSubCategoriesPagingQuery.Data>>>,
        categoryDialogInnerViewModelState: InnerViewModelState.CategorySelectDialog,
    ): CategorySelectDialogUiState {
        val categorySet = categoryDialogInnerViewModelState.categorySet

        fun changeRootScreen() {
            innerViewModelStateFlow.update {
                it.copy(
                    categorySelectDialog = categoryDialogInnerViewModelState.copy(
                        screenType = InnerViewModelState.CategorySelectDialog.ScreenType.Root,
                    ),
                )
            }
        }

        return CategorySelectDialogUiState(
            screenType = when (categoryDialogInnerViewModelState.screenType) {
                InnerViewModelState.CategorySelectDialog.ScreenType.Root -> {
                    CategorySelectDialogUiState.Screen.Root(
                        category = categorySet.category?.name ?: "未選択",
                        subCategory = categorySet.subCategory?.name ?: "未選択",
                        enableSubCategory = categorySet.category != null,
                        onClickCategory = {
                            innerViewModelStateFlow.update {
                                it.copy(
                                    categorySelectDialog = categoryDialogInnerViewModelState.copy(
                                        screenType = InnerViewModelState.CategorySelectDialog.ScreenType.Category,
                                    ),
                                )
                            }
                        },
                        onClickSubCategory = {
                            innerViewModelStateFlow.update {
                                it.copy(
                                    categorySelectDialog = categoryDialogInnerViewModelState.copy(
                                        screenType = InnerViewModelState.CategorySelectDialog.ScreenType.SubCategory,
                                    ),
                                )
                            }
                        },
                    )
                }

                InnerViewModelState.CategorySelectDialog.ScreenType.Category -> {
                    val categories: List<CategorySelectDialogUiState.Category> = when (categoriesApolloResponse) {
                        is ApolloResponseState.Failure -> {
                            listOf()
                        }

                        is ApolloResponseState.Loading -> {
                            listOf()
                        }

                        is ApolloResponseState.Success -> {
                            val response = categoriesApolloResponse.value.data?.user?.moneyUsageCategories
                            if (response == null) {
                                // TODO error handling
                                listOf()
                            } else {
                                response.nodes.map { item ->
                                    CategorySelectDialogUiState.Category(
                                        name = item.name,
                                        isSelected = item.id == categorySet.category?.id,
                                        onSelected = {
                                            event.categorySelected(item.id)

                                            innerViewModelStateFlow.update {
                                                it.copy(
                                                    categorySelectDialog = categoryDialogInnerViewModelState.copy(
                                                        screenType = InnerViewModelState.CategorySelectDialog.ScreenType.Root,
                                                        categorySet = categorySet.copy(
                                                            category = item,
                                                            subCategory = null,
                                                        ),
                                                    ),
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    CategorySelectDialogUiState.Screen.Category(
                        categories = categories.toImmutableList(),
                        onBackRequest = { changeRootScreen() },
                    )
                }

                InnerViewModelState.CategorySelectDialog.ScreenType.SubCategory -> {
                    val subCategories: List<CategorySelectDialogUiState.Category> = when (val subCategory = subCategoryApolloResponses[categorySet.category?.id]) {
                        null,
                        is ApolloResponseState.Loading,
                        -> {
                            listOf()
                        }

                        is ApolloResponseState.Failure -> {
                            // TODO Error handling
                            listOf()
                        }

                        is ApolloResponseState.Success -> {
                            val subCategories = subCategory.value.data?.user?.moneyUsageCategory?.subCategories
                            if (subCategories == null) {
                                // TODO Error handling
                                listOf()
                            } else {
                                subCategories.nodes.map { item ->
                                    CategorySelectDialogUiState.Category(
                                        name = item.name,
                                        isSelected = item.id == categorySet.subCategory?.id,
                                        onSelected = {
                                            innerViewModelStateFlow.update {
                                                it.copy(
                                                    categorySelectDialog = categoryDialogInnerViewModelState.copy(
                                                        screenType = net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel.InnerViewModelState.CategorySelectDialog.ScreenType.Root,
                                                        categorySet = categorySet.copy(
                                                            subCategory = item,
                                                        ),
                                                    ),
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    CategorySelectDialogUiState.Screen.SubCategory(
                        subCategories = subCategories.toImmutableList(),
                        onBackRequest = { changeRootScreen() },
                    )
                }
            },
            event = object : CategorySelectDialogUiState.Event {
                override fun dismissRequest() {
                    innerViewModelStateFlow.update {
                        it.copy(
                            categorySelectDialog = null,
                        )
                    }
                }

                override fun selectCompleted() {
                    innerViewModelStateFlow.update {
                        it.copy(
                            usageCategorySet = it.categorySelectDialog?.categorySet ?: return,
                            categorySelectDialog = null,
                        )
                    }
                }
            },
        )
    }

    interface Event {
        fun categorySelected(id: MoneyUsageCategoryId)
    }

    data class ViewModelState(
        val usageCategorySet: CategorySet = CategorySet(),
    )

    private data class InnerViewModelState(
        val usageCategorySet: CategorySet = CategorySet(),
        val categorySelectDialog: CategorySelectDialog? = null,
        val subCategories: Map<MoneyUsageCategoryId, ApolloResponseState<ApolloResponse<MoneyUsageSelectDialogSubCategoriesPagingQuery.Data>>> = mapOf(),
        val categories: ApolloResponseState<ApolloResponse<MoneyUsageSelectDialogCategoriesPagingQuery.Data>> = ApolloResponseState.loading(),
    ) {

        data class CategorySelectDialog(
            val categorySet: CategorySet,
            val screenType: ScreenType,
        ) {
            enum class ScreenType {
                Root,
                Category,
                SubCategory,
            }
        }
    }

    data class CategorySet(
        val category: MoneyUsageSelectDialogCategoriesPagingQuery.Node? = null,
        val subCategory: MoneyUsageSelectDialogSubCategoriesPagingQuery.Node? = null,
    )
}
