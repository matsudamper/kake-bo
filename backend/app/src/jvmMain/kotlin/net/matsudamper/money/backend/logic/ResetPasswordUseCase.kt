package net.matsudamper.money.backend.logic

import net.matsudamper.money.backend.app.interfaces.AdminRepository

class ResetPasswordUseCase(
    private val adminRepository: AdminRepository,
    private val passwordManager: IPasswordManager,
) {
    fun resetPassword(
        userName: String,
        password: String,
    ): Result {
        val errors = mutableListOf<Result.Errors>()

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

        val passwordResult = passwordManager.create(
            password = password,
            keyByteLength = 512,
            iterationCount = 100000,
            saltByteLength = 32,
            algorithm = IPasswordManager.Algorithm.PBKDF2WithHmacSHA512,
        )
        val resetResult = adminRepository.resetPassword(
            userName = userName,
            hashedPassword = passwordResult.hashedPassword,
            algorithmName = passwordResult.algorithm,
            salt = passwordResult.salt,
            iterationCount = passwordResult.iterationCount,
            keyLength = passwordResult.keyLength,
        )
        return when (resetResult) {
            is AdminRepository.ResetPasswordResult.Failed -> {
                when (val error = resetResult.error) {
                    is AdminRepository.ResetPasswordResult.ErrorType.InternalServerError -> {
                        error.e.printStackTrace()
                        Result.Failure(errors = listOf(Result.Errors.InternalServerError))
                    }
                }
            }

            AdminRepository.ResetPasswordResult.UserNotFound -> {
                Result.Failure(errors = listOf(Result.Errors.UserNotFound))
            }

            AdminRepository.ResetPasswordResult.Success -> {
                Result.Success
            }
        }
    }

    sealed interface Result {
        data object Success : Result

        class Failure(
            val errors: List<Errors>,
        ) : Result

        sealed interface Errors {
            data object PasswordLength : Errors

            class PasswordValidation(
                val errorChar: List<Char>,
            ) : Errors

            data object UserNotFound : Errors

            data object InternalServerError : Errors
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
