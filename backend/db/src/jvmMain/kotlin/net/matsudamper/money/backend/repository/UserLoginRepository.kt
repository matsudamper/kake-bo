package net.matsudamper.money.backend.repository

import java.security.spec.KeySpec
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import net.matsudamper.money.backend.DbConnectionImpl
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.element.UserId
import net.matsudamper.money.db.schema.tables.JUserPasswordExtendData
import net.matsudamper.money.db.schema.tables.JUserPasswords
import net.matsudamper.money.db.schema.tables.JUsers
import net.matsudamper.money.db.schema.tables.records.JUserPasswordExtendDataRecord
import net.matsudamper.money.db.schema.tables.records.JUserPasswordsRecord
import org.jooq.impl.DSL

class UserLoginRepository {
    private val user = JUsers.USERS
    private val userPasswords = JUserPasswords.USER_PASSWORDS
    private val userPasswordExtendData = JUserPasswordExtendData.USER_PASSWORD_EXTEND_DATA
    private val bases64Encoder = Base64.getEncoder()

    fun login(userName: String, passwords: String): Result {
        val result = DbConnectionImpl.use {
            DSL.using(it)
                .select(userPasswords, userPasswordExtendData)
                .from(user)
                .join(userPasswords)
                .on(userPasswords.USER_ID.eq(user.USER_ID))
                .join(userPasswordExtendData)
                .on(userPasswordExtendData.USER_ID.eq(userPasswords.USER_ID))
                .where(
                    user.USER_NAME.eq(userName),
                )
                .fetchOne()
        } ?: return Result.Failure

        val userPasswordRecord: JUserPasswordsRecord = result.value1()
        val userPasswordExtendDataRecord: JUserPasswordExtendDataRecord = result.value2()

        val spec: KeySpec = PBEKeySpec(
            passwords.plus(ServerEnv.userPasswordPepper).toCharArray(),
            userPasswordExtendDataRecord.salt!!,
            userPasswordExtendDataRecord.iterationCount!!,
            userPasswordExtendDataRecord.keyLength!!,
        )
        val factory = SecretKeyFactory.getInstance(userPasswordExtendDataRecord.algorithm!!)
        val hashedPassword = factory.generateSecret(spec).encoded

        return if (userPasswordRecord.passwordHash!! == bases64Encoder.encodeToString(hashedPassword)) {
            Result.Success(UserId(userPasswordRecord.userId!!))
        } else {
            Result.Failure
        }
    }

    sealed interface Result {
        data class Success(val uerId: UserId) : Result
        object Failure : Result
    }
}
