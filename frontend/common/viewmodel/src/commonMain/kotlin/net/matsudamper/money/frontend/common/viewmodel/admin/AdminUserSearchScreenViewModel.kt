package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminUserSearchUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

public class AdminUserSearchScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AdminUserSearchUiState> = MutableStateFlow(
        AdminUserSearchUiState(
            searchQuery = "",
            searchResults = emptyList(),
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
                        searchResults = viewModelState.searchResults,
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
                    val result = adminQuery.searchUsers(query)
                    val users = result.data?.adminMutation?.searchUsers?.users.orEmpty()
                    viewModelStateFlow.update { it.copy(searchResults = users) }
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
        }
    }

    private data class ViewModelState(
        val searchQuery: String = "",
        val searchResults: List<String> = emptyList(),
        val selectedUserName: String? = null,
        val showReplacePasswordDialog: Boolean = false,
        val password: String = "",
        val resultMessage: String? = null,
    )
}
