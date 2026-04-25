package net.matsudamper.money.backend.app.interfaces

interface AdminRepository {
    fun addUser(
        userName: String,
        hashedPassword: String,
        algorithmName: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
    ): AddUserResult

    fun searchUsers(query: String, size: Int, cursor: String?): SearchUsersResult

    data class SearchUsersResult(
        val users: List<String>,
        val cursor: String?,
        val hasMore: Boolean,
    )

    fun replacePassword(
        userName: String,
        hashedPassword: String,
        algorithmName: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
    ): ReplacePasswordResult

    sealed interface AddUserResult {
        data object Success : AddUserResult

        data class Failed(val error: ErrorType) : AddUserResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }

    sealed interface ReplacePasswordResult {
        data object Success : ReplacePasswordResult

        data object UserNotFound : ReplacePasswordResult

        data class Failed(val error: ErrorType) : ReplacePasswordResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }
}
