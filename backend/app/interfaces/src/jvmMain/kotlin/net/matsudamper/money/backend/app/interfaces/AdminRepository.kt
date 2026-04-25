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
        val users: List<User>,
        val cursor: String?,
        val hasMore: Boolean,
    )

    data class User(
        val userId: net.matsudamper.money.element.UserId,
        val name: String,
    )

    fun replacePassword(
        userId: net.matsudamper.money.element.UserId,
        hashedPassword: String,
        algorithmName: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
    ): ReplacePasswordResult

    fun deletePassword(userId: net.matsudamper.money.element.UserId): DeletePasswordResult

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

    sealed interface DeletePasswordResult {
        data object Success : DeletePasswordResult

        data object UserNotFound : DeletePasswordResult

        data class Failed(val error: ErrorType) : DeletePasswordResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }
}
