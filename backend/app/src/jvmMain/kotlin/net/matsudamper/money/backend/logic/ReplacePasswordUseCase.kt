package net.matsudamper.money.backend.logic

import net.matsudamper.money.backend.app.interfaces.AdminRepository
import net.matsudamper.money.element.UserId

class ReplacePasswordUseCase(
    private val adminRepository: AdminRepository,
    private val passwordManager: IPasswordManager,
) {
    fun replacePassword(
        userId: UserId,
        password: String,
    ): Result {
        val errors = mutableListOf<Result.Errors>()

        val passwordErrors = PasswordValidator.validate(password)
        for (passwordError in passwordErrors) {
            errors.add(
                when (passwordError) {
                    is PasswordValidator.Errors.PasswordLength -> {
                        Result.Errors.PasswordLength
                    }

                    is PasswordValidator.Errors.PasswordValidation -> {
                        Result.Errors.PasswordValidation(passwordError.errorChar)
                    }
                },
            )
        }

        if (errors.isNotEmpty()) {
            return Result.Failure(errors = errors)
        }

        val passwordResult = PasswordConstraints.createHash(passwordManager, password)
        val replaceResult = adminRepository.replacePassword(
            userId = userId,
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
}
