package net.matsudamper.money.backend.logic

import net.matsudamper.money.backend.app.interfaces.AdminRepository

class ReplacePasswordUseCase(
    private val adminRepository: AdminRepository,
    private val passwordManager: IPasswordManager,
) {
    fun replacePassword(
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
        val replaceResult = adminRepository.replacePassword(
            userName = userName,
            hashedPassword = passwordResult.hashedPassword,
            algorithmName = passwordResult.algorithm,
            salt = passwordResult.salt,
            iterationCount = passwordResult.iterationCount,
            keyLength = passwordResult.keyLength,
        )
        return when (replaceResult) {
            is AdminRepository.ReplacePasswordResult.Failed -> {
                when (val error = replaceResult.error) {
                    is AdminRepository.ReplacePasswordResult.ErrorType.InternalServerError -> {
                        error.e.printStackTrace()
                        Result.Failure(errors = listOf(Result.Errors.InternalServerError))
                    }
                }
            }

            AdminRepository.ReplacePasswordResult.UserNotFound -> {
                Result.Failure(errors = listOf(Result.Errors.UserNotFound))
            }

            AdminRepository.ReplacePasswordResult.Success -> {
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
