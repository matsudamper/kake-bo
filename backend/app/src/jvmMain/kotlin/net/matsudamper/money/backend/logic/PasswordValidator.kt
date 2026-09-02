package net.matsudamper.money.backend.logic

object PasswordValidator {
    fun validate(password: String): List<Errors> {
        return buildList {
            if (password.length !in PasswordConstraints.LENGTH_RANGE) {
                add(Errors.PasswordLength)
            }
            val denyChars = PasswordConstraints.findDenyChars(password)
            if (denyChars.isNotEmpty()) {
                add(Errors.PasswordValidation(denyChars))
            }
        }
    }

    sealed interface Errors {
        data object PasswordLength : Errors

        class PasswordValidation(
            val errorChar: List<Char>,
        ) : Errors
    }
}
