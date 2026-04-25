package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminUserSearchUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.AdminSearchUsersMutation
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

public class AdminUserSearchScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
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
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        searchQuery = viewModelState.searchQuery,
                        searchResults = viewModelState.searchResults.map { AdminUserSearchUiState.SearchResult(name = it.name) },
                        hasMore = viewModelState.hasMore,
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
                viewModelScope.launch {
                    val query = viewModelStateFlow.value.searchQuery
                    val result = adminQuery.searchUsers(
                        query = query,
                        size = PAGE_SIZE,
                        cursor = null,
                    )
                    val searchUsers = result.data?.adminMutation?.searchUsers
                    viewModelStateFlow.update {
                        it.copy(
                            committedQuery = query,
                            searchResults = searchUsers?.nodes.orEmpty(),
                            hasMore = searchUsers?.hasMore == true,
                            cursor = searchUsers?.cursor,
                        )
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
                viewModelScope.launch {
                    val state = viewModelStateFlow.value
                    val cursor = state.cursor ?: return@launch
                    viewModelStateFlow.update { it.copy(isLoadingMore = true) }
                    val result = adminQuery.searchUsers(
                        query = state.committedQuery,
                        size = PAGE_SIZE,
                        cursor = cursor,
                    )
                    val searchUsers = result.data?.adminMutation?.searchUsers
                    viewModelStateFlow.update {
                        it.copy(
                            searchResults = it.searchResults + searchUsers?.nodes.orEmpty(),
                            hasMore = searchUsers?.hasMore == true,
                            cursor = searchUsers?.cursor,
                            isLoadingMore = false,
                        )
                    }
                }
            }
        }
    }

    private data class ViewModelState(
        val searchQuery: String = "",
        val committedQuery: String = "",
        val searchResults: List<AdminSearchUsersMutation.Node> = listOf(),
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val cursor: String? = null,
        val selectedUserName: String? = null,
        val showReplacePasswordDialog: Boolean = false,
        val password: String = "",
        val resultMessage: String? = null,
    )

    private companion object {
        const val PAGE_SIZE = 20
    }
}
