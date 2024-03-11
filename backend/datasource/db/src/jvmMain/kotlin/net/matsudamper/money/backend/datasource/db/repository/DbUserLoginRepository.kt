package net.matsudamper.money.backend.datasource.db.repository

import java.util.Base64
import net.matsudamper.money.backend.app.interfaces.UserLoginRepository
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.db.schema.tables.JUserPasswordExtendData
import net.matsudamper.money.db.schema.tables.JUserPasswords
import net.matsudamper.money.db.schema.tables.JUsers
import net.matsudamper.money.db.schema.tables.records.JUserPasswordExtendDataRecord
import net.matsudamper.money.db.schema.tables.records.JUserPasswordsRecord
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbUserLoginRepository(
    private val dbConnection: DbConnection,
) : UserLoginRepository {
    private val user = JUsers.USERS
    private val userPasswords = JUserPasswords.USER_PASSWORDS
    private val userPasswordExtendData = JUserPasswordExtendData.USER_PASSWORD_EXTEND_DATA
    private val bases64Encoder = Base64.getEncoder()

    override fun getLoginEncryptInfo(userName: String): UserLoginRepository.LoginEncryptInfo? {
        val result = dbConnection.use {
            DSL.using(it)
                .select(userPasswordExtendData)
                .from(user)
                .join(userPasswordExtendData).on(user.USER_ID.eq(userPasswordExtendData.USER_ID))
                .where(user.USER_NAME.eq(userName))
                .fetchOne()
        } ?: return null

        val userPasswordExtendDataRecord: JUserPasswordExtendDataRecord = result.value1()

        return UserLoginRepository.LoginEncryptInfo(
            salt = userPasswordExtendDataRecord.salt!!,
            algorithm = userPasswordExtendDataRecord.algorithm!!,
            iterationCount = userPasswordExtendDataRecord.iterationCount!!,
            keyLength = userPasswordExtendDataRecord.keyLength!!,
        )
    }

    override fun login(
        userName: String,
        hashedPassword: ByteArray,
    ): UserLoginRepository.Result {
        val result = dbConnection.use {
            DSL.using(it)
                .select(userPasswords)
                .from(user)
                .join(userPasswords)
                .on(userPasswords.USER_ID.eq(user.USER_ID))
                .where(user.USER_NAME.eq(userName))
                .fetchOne()
        } ?: return UserLoginRepository.Result.Failure

        val userPasswordRecord: JUserPasswordsRecord = result.value1()

        return if (userPasswordRecord.passwordHash!! == bases64Encoder.encodeToString(hashedPassword)) {
            UserLoginRepository.Result.Success(UserId(userPasswordRecord.userId!!))
        } else {
            UserLoginRepository.Result.Failure
        }
    }
}
