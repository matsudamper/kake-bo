package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.UserId

interface UserLoginRepository {
    sealed interface Result {
        data class Success(val uerId: UserId) : Result

        data object Failure : Result
    }

    fun login(
        userName: String,
        passwords: String,
    ): Result
}
