package net.matsudamper.money.backend.logic

internal object PasswordConstraints {
    val LENGTH_RANGE: IntRange = 20..256

    internal val alphaNumerics: List<Char> = ('A'..'Z')
        .plus('a'..'z')
        .plus('0'..'9')

    val allowSymbols: Set<Char> = setOf(
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

    fun findDenyChars(password: String): List<Char> {
        return password.toList().filter { it !in alphaNumerics && it !in allowSymbols }
    }

    fun createHash(
        passwordManager: IPasswordManager,
        password: String,
    ): IPasswordManager.CreateResult {
        return passwordManager.create(
            password = password,
            keyByteLength = 512,
            iterationCount = 100000,
            saltByteLength = 32,
            algorithm = IPasswordManager.Algorithm.PBKDF2WithHmacSHA512,
        )
    }
}
