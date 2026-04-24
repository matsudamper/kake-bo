package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.AdminLoginRepository
import net.matsudamper.money.backend.base.ServerEnv

class EnvAdminLoginRepository : AdminLoginRepository {
    override fun getLoginEncryptInfo(): AdminLoginRepository.LoginEncryptInfo? {
        val saltHex = ServerEnv.adminPasswordSalt ?: return null
        val keyLength = ServerEnv.adminPasswordKeyLength ?: return null

        val salt = try {
            saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (_: NumberFormatException) {
            return null
        }
        return AdminLoginRepository.LoginEncryptInfo(
            salt = salt,
            algorithm = ServerEnv.adminPasswordAlgorithm,
            iterationCount = ServerEnv.adminPasswordIterationCount,
            keyLength = keyLength,
        )
    }

    override fun verifyPassword(hashedPassword: String): Boolean {
        val expected = ServerEnv.adminPasswordHash ?: return false
        return hashedPassword == expected
    }
}
