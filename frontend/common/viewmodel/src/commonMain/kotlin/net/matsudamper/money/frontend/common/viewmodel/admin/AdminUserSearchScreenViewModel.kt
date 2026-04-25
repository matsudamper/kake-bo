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
import net.matsudamper.money.frontend.common.ui.screen.admin.user.AdminUserSearchUiState
import net.matsudamper.money.frontend.common.ui.screen.admin.user.AdminUserSearchUiState.ReplacePasswordDialogState
import net.matsudamper.money.frontend.common.ui.screen.admin.user.UserOperationDialogState
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
    private var searchJob: kotlinx.coroutines.Job? = null
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AdminUserSearchUiState> = MutableStateFlow(
        AdminUserSearchUiState(
            searchQuery = "",
            users = listOf(),
            hasMore = false,
            userOperationDialogUiState = null,
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
                        users = searchUsersConnection?.nodes.orEmpty().map { user ->
                            createUserItem(user = user)
                        },
                        hasMore = searchUsersConnection?.hasMore == true,
                        userOperationDialogUiState = viewModelState.userOperationDialogUiState,
                        replacePasswordDialogState = viewModelState.replacePasswordDialogState,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createUserItem(
        user: AdminSearchUsersQuery.Node,
    ): AdminUserSearchUiState.User {
        return AdminUserSearchUiState.User(
            displayId = user.userId.value.toString(),
            name = user.name,
            listener = object : AdminUserSearchUiState.User.Listener {
                override fun onClick() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            userOperationDialogUiState = createUserOperationDialogState(
                                user = user,
                            ),
                        )
                    }
                }

                override fun dismiss() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            userOperationDialogUiState = null,
                        )
                    }
                }
            },
        )
    }

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

                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    viewModelStateFlow.update {
                        it.copy(
                            committedQuery = firstQuery,
                        )
                    }

                    launch {
                        pagingModel.getFlow(firstQuery).collectLatest { response ->
                            viewModelStateFlow.update { it.copy(apolloResponse = response) }
                        }
                    }

                    launch {
                        val result = pagingModel.refresh(firstQuery)
                        if (result is UpdateOperationResponseResult.Error<*>) {
                            result.e?.let { Logger.e(TAG, it) }
                        }
                    }
                }
            }

            override fun loadMore() {
                if (viewModelStateFlow.value.isLoadingMore) return
                val committedQuery = viewModelStateFlow.value.committedQuery ?: return

                viewModelScope.launch {
                    viewModelStateFlow.update { it.copy(isLoadingMore = true) }

                    try {
                        val result = pagingModel.fetch(committedQuery)
                        if (result is UpdateOperationResponseResult.Error<*>) {
                            result.e?.let { Logger.e(TAG, it) }
                        }
                    } finally {
                        viewModelStateFlow.update {
                            it.copy(isLoadingMore = false)
                        }
                    }
                }
            }
        }
    }

    private fun createUserOperationDialogState(
        user: AdminSearchUsersQuery.Node,
    ): UserOperationDialogState {
        return UserOperationDialogState(
            userName = user.name,
            listener = object : UserOperationDialogState.Listener {
                override fun onDismissUserMenu() {
                    viewModelStateFlow.update {
                        it.copy(
                            userOperationDialogUiState = null,
                        )
                    }
                }

                override fun onClickReplacePassword() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            replacePasswordDialogState = createReplacePasswordDialogState(
                                user = user,
                            ),
                            userOperationDialogUiState = null,
                        )
                    }
                }
            },
        )
    }

    private fun createReplacePasswordDialogState(
        user: AdminSearchUsersQuery.Node,
    ): ReplacePasswordDialogState {
        return ReplacePasswordDialogState(
            userName = user.name,
            resultMessage = null,
            listener = object : ReplacePasswordDialogState.Listener {
                override fun passwordInputDone(password: String) {
                    passwordChange(
                        user = user,
                        password = password,
                    )
                }

                override fun dismiss() {
                    viewModelStateFlow.update {
                        it.copy(
                            replacePasswordDialogState = null,
                        )
                    }
                }
            },
        )
    }

    private fun passwordChange(
        user: AdminSearchUsersQuery.Node,
        password: String,
    ) {
        viewModelScope.launch {
            val result = adminQuery.replacePassword(
                userName = user.name,
                password = password,
            )
            val data = result.data?.adminMutation?.replacePassword
            val message = if (data?.isSuccess == true) {
                "パスワードを変更しました"
            } else {
                when (data?.errorType?.rawValue) {
                    "UserNotFound" -> "ユーザーが見つかりません"
                    "PasswordLength" -> "パスワードの長さが不正です（20〜256文字）"
                    "PasswordInvalidChar" -> "パスワードに使用できない文字が含まれています"
                    else -> "エラーが発生しました"
                }
            }
            viewModelStateFlow.update {
                it.copy(resultMessage = message)
            }
        }
    }

    private data class ViewModelState(
        val searchQuery: String = "",
        val committedQuery: AdminSearchUsersQuery? = null,
        val apolloResponse: ApolloResponse<AdminSearchUsersQuery.Data>? = null,
        val isLoadingMore: Boolean = false,
        val replacePasswordDialogState: ReplacePasswordDialogState? = null,
        val userOperationDialogUiState: UserOperationDialogState? = null,
        val resultMessage: String? = null,
    )

    private companion object {
        const val PAGE_SIZE = 20
    }
}
