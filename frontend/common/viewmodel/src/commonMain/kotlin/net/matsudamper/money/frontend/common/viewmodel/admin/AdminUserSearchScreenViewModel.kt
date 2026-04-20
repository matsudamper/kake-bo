package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        createUiState(),
    )

    init {
        viewModelScope.launch {
            viewModelStateFlow.collect {
                (uiStateFlow as MutableStateFlow).value = createUiState()
            }
        }
    }

    private fun createUiState(): AdminUserSearchUiState {
        val state = viewModelStateFlow.value
        return AdminUserSearchUiState(
            searchResults = state.searchResults,
            selectedUserName = state.selectedUserName,
            resetPasswordDialogState = if (state.showResetPasswordDialog && state.selectedUserName != null) {
                AdminUserSearchUiState.ResetPasswordDialogState(
                    userName = state.selectedUserName,
                    resultMessage = state.resultMessage,
                )
            } else {
                null
            },
            listener = object : AdminUserSearchUiState.Listener {
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

                override fun onClickResetPassword() {
                    viewModelStateFlow.update {
                        it.copy(
                            showResetPasswordDialog = true,
                            password = "",
                            resultMessage = null,
                        )
                    }
                }

                override fun onPasswordChanged(password: String) {
                    viewModelStateFlow.update { it.copy(password = password) }
                }

                override fun onClickSubmitResetPassword() {
                    viewModelScope.launch {
                        val state = viewModelStateFlow.value
                        val userName = state.selectedUserName ?: return@launch
                        val result = adminQuery.resetPassword(
                            userName = userName,
                            password = state.password,
                        )
                        val data = result.data?.adminMutation?.resetPassword
                        if (data?.isSuccess == true) {
                            viewModelStateFlow.update {
                                it.copy(
                                    showResetPasswordDialog = false,
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

                override fun onDismissResetPasswordDialog() {
                    viewModelStateFlow.update {
                        it.copy(
                            showResetPasswordDialog = false,
                            selectedUserName = null,
                            password = "",
                            resultMessage = null,
                        )
                    }
                }
            },
        )
    }

    private data class ViewModelState(
        val searchQuery: String = "",
        val searchResults: List<String> = emptyList(),
        val selectedUserName: String? = null,
        val showResetPasswordDialog: Boolean = false,
        val password: String = "",
        val resultMessage: String? = null,
    )
}
