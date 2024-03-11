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

    sealed interface AddUserResult {
        data object Success : AddUserResult

        data class Failed(val error: ErrorType) : AddUserResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }
}
