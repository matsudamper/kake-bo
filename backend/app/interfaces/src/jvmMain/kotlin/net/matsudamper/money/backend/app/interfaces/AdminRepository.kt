package net.matsudamper.money.backend.app.interfaces

interface AdminRepository {
    fun addUser(
        userName: String,
        password: String,
    ): AddUserResult

    sealed interface AddUserResult {
        data object Success : AddUserResult

        data class Failed(val error: ErrorType) : AddUserResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }
}
