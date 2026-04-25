package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminUserSearchUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.AdminSearchUsersQuery
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery
import net.matsudamper.money.frontend.graphql.UpdateOperationResponseResult

private const val TAG = "AdminUserSearchScreenViewModel"

public class AdminUserSearchScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
    private val pagingModel: AdminUserSearchPagingModel,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AdminUserSearchUiState> = MutableStateFlow(
        AdminUserSearchUiState(
            searchQuery = "",
            searchResults = listOf(),
            hasMore = false,
            selectedUserName = null,
            replacePasswordDialogState = null,
            listener = createListener(),
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                val response = viewModelState.apolloResponse
                val searchUsersConnection = response?.data?.adminSearchUsers

                uiStateFlow.update { uiState ->
                    uiState.copy(
                        searchQuery = viewModelState.searchQuery,
                        searchResults = searchUsersConnection?.nodes.orEmpty().map { AdminUserSearchUiState.SearchResult(name = it.name) },
                        hasMore = searchUsersConnection?.hasMore == true,
                        selectedUserName = viewModelState.selectedUserName,
                        replacePasswordDialogState = if (viewModelState.showReplacePasswordDialog && viewModelState.selectedUserName != null) {
                            AdminUserSearchUiState.ReplacePasswordDialogState(
                                userName = viewModelState.selectedUserName,
                                password = viewModelState.password,
                                resultMessage = viewModelState.resultMessage,
                            )
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createListener(): AdminUserSearchUiState.Listener {
        return object : AdminUserSearchUiState.Listener {
            override fun onSearchQueryChanged(query: String) {
                viewModelStateFlow.update { it.copy(searchQuery = query) }
            }

            override fun onClickSearch() {
                val query = viewModelStateFlow.value.searchQuery
                val firstQuery = AdminSearchUsersQuery(
                    query = query,
                    size = PAGE_SIZE,
                    cursor = Optional.present(null),
                )

                viewModelStateFlow.update {
                    it.copy(
                        committedQuery = firstQuery,
                    )
                }

                viewModelScope.launch {
                    pagingModel.getFlow(firstQuery).collectLatest { response ->
                        viewModelStateFlow.update { it.copy(apolloResponse = response) }
                    }
                }

                viewModelScope.launch {
                    val result = pagingModel.refresh(firstQuery)
                    if (result is UpdateOperationResponseResult.Error<*>) {
                        result.e?.let { Logger.e(TAG, it) }
                    }
                }
            }

            override fun onClickUser(userName: String) {
                viewModelStateFlow.update { it.copy(selectedUserName = userName) }
            }

            override fun onDismissUserMenu() {
                viewModelStateFlow.update { it.copy(selectedUserName = null) }
            }

            override fun onClickReplacePassword() {
                viewModelStateFlow.update {
                    it.copy(
                        showReplacePasswordDialog = true,
                        password = "",
                        resultMessage = null,
                    )
                }
            }

            override fun onPasswordChanged(password: String) {
                viewModelStateFlow.update { it.copy(password = password) }
            }

            override fun onClickSubmitReplacePassword() {
                viewModelScope.launch {
                    val state = viewModelStateFlow.value
                    val userName = state.selectedUserName ?: return@launch
                    val result = adminQuery.replacePassword(
                        userName = userName,
                        password = state.password,
                    )
                    val data = result.data?.adminMutation?.replacePassword
                    if (data?.isSuccess == true) {
                        viewModelStateFlow.update {
                            it.copy(
                                showReplacePasswordDialog = false,
                                selectedUserName = null,
                                password = "",
                                resultMessage = null,
                            )
                        }
                    } else {
                        val errorMessage = when (data?.errorType?.rawValue) {
                            "UserNotFound" -> "ユーザーが見つかりません"
                            "PasswordLength" -> "パスワードの長さが不正です（20〜256文字）"
                            "PasswordInvalidChar" -> "パスワードに使用できない文字が含まれています"
                            else -> "エラーが発生しました"
                        }
                        viewModelStateFlow.update {
                            it.copy(resultMessage = errorMessage)
                        }
                    }
                }
            }

            override fun onDismissReplacePasswordDialog() {
                viewModelStateFlow.update {
                    it.copy(
                        showReplacePasswordDialog = false,
                        selectedUserName = null,
                        password = "",
                        resultMessage = null,
                    )
                }
            }

            override fun onClickLoadMore() {
                if (viewModelStateFlow.value.isLoadingMore) return
                val committedQuery = viewModelStateFlow.value.committedQuery ?: return

                viewModelScope.launch {
                    viewModelStateFlow.update { it.copy(isLoadingMore = true) }

                    val result = pagingModel.fetch(committedQuery)
                    if (result is UpdateOperationResponseResult.Error<*>) {
                        result.e?.let { Logger.e(TAG, it) }
                    }

                    viewModelStateFlow.update {
                        it.copy(isLoadingMore = false)
                    }
                }
            }
        }
    }

    private data class ViewModelState(
        val searchQuery: String = "",
        val committedQuery: AdminSearchUsersQuery? = null,
        val apolloResponse: ApolloResponse<AdminSearchUsersQuery.Data>? = null,
        val isLoadingMore: Boolean = false,
        val selectedUserName: String? = null,
        val showReplacePasswordDialog: Boolean = false,
        val password: String = "",
        val resultMessage: String? = null,
    )

    private companion object {
        const val PAGE_SIZE = 20
    }
}
