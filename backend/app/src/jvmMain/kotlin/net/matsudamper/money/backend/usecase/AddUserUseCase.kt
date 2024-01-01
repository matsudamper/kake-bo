package net.matsudamper.money.backend.usecase

import net.matsudamper.money.backend.datasource.db.repository.AdminRepository

class AddUserUseCase(
    private val adminRepository: AdminRepository = AdminRepository(),
) {

    fun addUser(userName: String, password: String): Result {
        val errors = mutableListOf<Result.Errors>()
        if (userName.length !in 3..20) {
            errors.add(Result.Errors.UserNameLength)
        } else if (userName.any { it !in nonSymbolList }) {
            errors.add(Result.Errors.UserNameValidation)
        }

        if (password.length !in 20..256) {
            errors.add(Result.Errors.PasswordLength)
        } else {
            val denyChars = password.toCharArray()
                .filterNot { it in nonSymbolList }
                .filterNot { it in passwordAllowSymbolList }
            if (denyChars.isNotEmpty()) {
                errors.add(Result.Errors.PasswordValidation(denyChars))
            }
        }

        if (errors.isNotEmpty()) {
            return Result.Failure(errors = errors)
        }

        val addUserResult = adminRepository.addUser(
            userName = userName,
            password = password,
        )
        return when (addUserResult) {
            is AdminRepository.AddUserResult.Failed -> {
                when (val error = addUserResult.error) {
                    is AdminRepository.AddUserResult.ErrorType.InternalServerError -> {
                        error.e.printStackTrace()
                        Result.Failure(errors = listOf(Result.Errors.InternalServerError))
                    }
                }
            }

            AdminRepository.AddUserResult.Success -> {
                Result.Success
            }
        }
    }

    sealed interface Result {
        object Success : Result
        class Failure(
            val errors: List<Errors>,
        ) : Result

        sealed interface Errors {
            object PasswordLength : Errors
            class PasswordValidation(
                val errorChar: List<Char>,
            ) : Errors

            object UserNameLength : Errors
            object UserNameValidation : Errors
            object InternalServerError : Errors
        }
    }

    companion object {
        private val nonSymbolList = ('A'..'Z')
            .plus('a'..'z')
            .plus('0'..'9')

        private val passwordAllowSymbolList = setOf(
            '!',
            '@',
            '#',
            '$',
            '%',
            '^',
            '&',
            '*',
            '(',
            ')',
            '_',
            '+',
            '-',
            '?',
            '<',
            '>',
            ',',
            '.',
        )
    }
}
