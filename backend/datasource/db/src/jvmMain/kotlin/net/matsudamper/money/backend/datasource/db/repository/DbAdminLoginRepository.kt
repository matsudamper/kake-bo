package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.AdminLoginRepository
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.db.schema.tables.JAdminPasswords
import net.matsudamper.money.db.schema.tables.records.JAdminPasswordsRecord
import org.jooq.impl.DSL

class DbAdminLoginRepository(
    private val dbConnection: DbConnection,
) : AdminLoginRepository {
    private val adminPasswords = JAdminPasswords.ADMIN_PASSWORDS

    override fun getLoginEncryptInfo(): AdminLoginRepository.LoginEncryptInfo? {
        val result = dbConnection.use {
            DSL.using(it)
                .select(adminPasswords)
                .from(adminPasswords)
                .orderBy(adminPasswords.ID.desc())
                .limit(1)
                .fetchOne()
        } ?: return null

        val record: JAdminPasswordsRecord = result.value1()

        return AdminLoginRepository.LoginEncryptInfo(
            salt = record.salt!!,
            algorithm = record.algorithm!!,
            iterationCount = record.iterationCount!!,
            keyLength = record.keyLength!!,
        )
    }

    override fun verifyPassword(hashedPassword: String): Boolean {
        val result = dbConnection.use {
            DSL.using(it)
                .select(adminPasswords.PASSWORD_HASH)
                .from(adminPasswords)
                .orderBy(adminPasswords.ID.desc())
                .limit(1)
                .fetchOne()
        } ?: return false

        return result.value1() == hashedPassword
    }
}
