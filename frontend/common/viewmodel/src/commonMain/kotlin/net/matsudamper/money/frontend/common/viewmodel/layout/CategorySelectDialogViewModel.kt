package net.matsudamper.money.frontend.common.viewmodel.layout

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
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.ViewModelFeature
import net.matsudamper.money.frontend.graphql.MoneyUsageSelectDialogCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.MoneyUsageSelectDialogSubCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryQuery

internal class CategorySelectDialogViewModel(
    viewModelFeature: ViewModelFeature,
    private val apolloClient: ApolloClient,
    private val event: Event,
) : CommonViewModel(viewModelFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val apolloResponseCollector =
        ApolloResponseCollector.create(
            apolloClient = apolloClient,
            query =
            MoneyUsageSelectDialogCategoriesPagingQuery(
                MoneyUsageCategoriesInput(
                    size = 100,
                    cursor = Optional.present(null),
                ),
            ),
        )
    private val subCategoriesFlow: MutableStateFlow<Map<MoneyUsageCategoryId, ApolloResponseCollector<MoneyUsageSelectDialogSubCategoriesPagingQuery.Data>>> =
        MutableStateFlow(mapOf())

    suspend fun getUiStateFlow(): StateFlow<CategorySelectDialogUiState?> {
        return channelFlow {
            viewModelStateFlow.collectLatest { viewModelState ->
                send(
                    if (viewModelState.categorySelectDialog != null) {
                        createCategorySelectDialogUiState(
                            categoriesApolloResponse = viewModelState.categories,
                            categoryDialogViewModelState = viewModelState.categorySelectDialog,
                            subCategoryApolloResponses = viewModelState.subCategories,
                        )
                    } else {
                        null
                    },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    }

    init {
        viewModelScope.launch {
            apolloResponseCollector.getFlow().collectLatest { apolloResponseState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        categories = apolloResponseState,
                    )
                }
            }
        }
        viewModelScope.launch {
            subCategoriesFlow.collectLatest { map ->
                combine(map.toList().map { pair -> pair.second.getFlow().map { pair.first to it } }) {
                    it
                }.collectLatest {

                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            subCategories = it.toMap(),
                        )
                    }
                }
            }
        }
    }

    fun showDialog(
        categoryId: MoneyUsageCategoryId? = null,
        categoryName: String? = null,
        subCategoryId: MoneyUsageSubCategoryId? = null,
        subCategoryName: String? = null,
        useCache: Boolean = true,
    ) {
        val category =
            run category@{
                ViewModelState.Category(
                    id = categoryId ?: return@category null,
                    name = categoryName ?: return@category null,
                )
            }
        val subCategory =
            run subCategory@{
                ViewModelState.SubCategory(
                    id = subCategoryId ?: return@subCategory null,
                    name = subCategoryName ?: return@subCategory null,
                )
            }
        viewModelStateFlow.update {
            it.copy(
                categorySelectDialog =
                ViewModelState.CategorySelectDialog(
                    categorySet =
                    ViewModelState.CategorySet(
                        category = category,
                        subCategory = subCategory,
                    ),
                    screenType = ViewModelState.CategorySelectDialog.ScreenType.Root,
                ),
            )
        }

        if (
            category == null ||
            useCache ||
            viewModelStateFlow.value.categories.getSuccessOrNull()
                ?.value?.data?.user?.moneyUsageCategories?.nodes.orEmpty()
                .any { it.id == categoryId }.not()
        ) {
            // TODO ページング
            // TODO error handling
            viewModelScope.launch {
                apolloResponseCollector.fetch()
            }
        }
        if (subCategory != null && categoryId != null && viewModelStateFlow.value.subCategories[categoryId] == null) {
            fetchSubCategories(categoryId)
        }
    }

    fun dismissDialog() {
        viewModelStateFlow.update {
            it.copy(
                categorySelectDialog = null,
            )
        }
    }

    // TODO ページング
    // TODO error handling
    private fun fetchSubCategories(id: MoneyUsageCategoryId) {
        val beforeItem = subCategoriesFlow.value[id]
        if (beforeItem != null) {
            viewModelScope.launch {
                beforeItem.fetch()
            }
            return
        }

        val collector =
            ApolloResponseCollector.create(
                apolloClient = apolloClient,
                query =
                MoneyUsageSelectDialogSubCategoriesPagingQuery(
                    categoryId = id,
                    query =
                    MoneyUsageSubCategoryQuery(
                        size = 100,
                        cursor = Optional.present(null),
                    ),
                ),
            )

        subCategoriesFlow.update { subCategories ->
            subCategories.plus(id to collector)
        }

        viewModelScope.launch {
            collector.fetch()
        }
    }

    private fun createCategorySelectDialogUiState(
        categoriesApolloResponse: ApolloResponseState<ApolloResponse<MoneyUsageSelectDialogCategoriesPagingQuery.Data>>,
        subCategoryApolloResponses: Map<MoneyUsageCategoryId, ApolloResponseState<ApolloResponse<MoneyUsageSelectDialogSubCategoriesPagingQuery.Data>>>,
        categoryDialogViewModelState: ViewModelState.CategorySelectDialog,
    ): CategorySelectDialogUiState {
        val categorySet = categoryDialogViewModelState.categorySet

        fun changeRootScreen() {
            viewModelStateFlow.update {
                it.copy(
                    categorySelectDialog =
                    categoryDialogViewModelState.copy(
                        screenType = ViewModelState.CategorySelectDialog.ScreenType.Root,
                    ),
                )
            }
        }

        return CategorySelectDialogUiState(
            screenType =
            when (categoryDialogViewModelState.screenType) {
                ViewModelState.CategorySelectDialog.ScreenType.Root -> {
                    CategorySelectDialogUiState.Screen.Root(
                        category = categorySet.category?.name ?: "未選択",
                        subCategory = categorySet.subCategory?.name ?: "未選択",
                        enableSubCategory = categorySet.category != null,
                        onClickCategory = {
                            viewModelStateFlow.update {
                                it.copy(
                                    categorySelectDialog =
                                    categoryDialogViewModelState.copy(
                                        screenType = ViewModelState.CategorySelectDialog.ScreenType.Category,
                                    ),
                                )
                            }
                        },
                        onClickSubCategory = {
                            viewModelStateFlow.update {
                                it.copy(
                                    categorySelectDialog =
                                    categoryDialogViewModelState.copy(
                                        screenType = ViewModelState.CategorySelectDialog.ScreenType.SubCategory,
                                    ),
                                )
                            }
                        },
                    )
                }

                ViewModelState.CategorySelectDialog.ScreenType.Category -> {
                    val categories: List<CategorySelectDialogUiState.Category> =
                        when (categoriesApolloResponse) {
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
                                                fetchSubCategories(item.id)
                                                viewModelStateFlow.update {
                                                    it.copy(
                                                        categorySelectDialog =
                                                        categoryDialogViewModelState.copy(
                                                            screenType = ViewModelState.CategorySelectDialog.ScreenType.Root,
                                                            categorySet =
                                                            ViewModelState.CategorySet(
                                                                category =
                                                                ViewModelState.Category(
                                                                    id = item.id,
                                                                    name = item.name,
                                                                ),
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

                ViewModelState.CategorySelectDialog.ScreenType.SubCategory -> {
                    val subCategories: List<CategorySelectDialogUiState.Category> =
                        when (val subCategory = subCategoryApolloResponses[categorySet.category?.id]) {
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
                                                viewModelStateFlow.update {
                                                    it.copy(
                                                        categorySelectDialog =
                                                        categoryDialogViewModelState.copy(
                                                            screenType = CategorySelectDialogViewModel.ViewModelState.CategorySelectDialog.ScreenType.Root,
                                                            categorySet =
                                                            categorySet.copy(
                                                                subCategory =
                                                                ViewModelState.SubCategory(
                                                                    id = item.id,
                                                                    name = item.name,
                                                                ),
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
            event =
            object : CategorySelectDialogUiState.Event {
                override fun dismissRequest() {
                    viewModelStateFlow.update {
                        it.copy(
                            categorySelectDialog = null,
                        )
                    }
                }

                override fun selectCompleted() {
                    event.selected(categorySet.toResult() ?: return)
                }
            },
        )
    }

    interface Event {
        fun selected(result: SelectedResult)
    }

    private data class ViewModelState(
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

        data class Category(
            val id: MoneyUsageCategoryId,
            val name: String,
        )

        data class SubCategory(
            val id: MoneyUsageSubCategoryId,
            val name: String,
        )

        data class CategorySet(
            val category: Category?,
            val subCategory: SubCategory?,
        ) {
            fun toResult(): SelectedResult? {
                category ?: return null
                subCategory ?: return null
                return SelectedResult(
                    categoryId = category.id,
                    categoryName = category.name,
                    subCategoryId = subCategory.id,
                    subCategoryName = subCategory.name,
                )
            }
        }
    }

    data class SelectedResult(
        val categoryId: MoneyUsageCategoryId,
        val categoryName: String,
        val subCategoryId: MoneyUsageSubCategoryId,
        val subCategoryName: String,
    )
}
