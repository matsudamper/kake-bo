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

    fun searchUsers(query: String): List<String>

    fun resetPassword(
        userName: String,
        hashedPassword: String,
        algorithmName: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
    ): ResetPasswordResult

    sealed interface AddUserResult {
        data object Success : AddUserResult

        data class Failed(val error: ErrorType) : AddUserResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }

    sealed interface ResetPasswordResult {
        data object Success : ResetPasswordResult

        data object UserNotFound : ResetPasswordResult

        data class Failed(val error: ErrorType) : ResetPasswordResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }
}
