package net.matsudamper.money.backend.repository

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.db.schema.tables.JUserPasswordExtendData
import net.matsudamper.money.db.schema.tables.JUserPasswords
import net.matsudamper.money.db.schema.tables.JUsers
import org.jooq.DSLContext
import org.jooq.impl.DSL


class AdminRepository {
    private val users = JUsers.USERS
    private val userPasswords = JUserPasswords.USER_PASSWORDS
    private val userPasswordExtendData = JUserPasswordExtendData.USER_PASSWORD_EXTEND_DATA
    private val bases64Encoder = Base64.getEncoder()

    fun addUser(userName: String, password: String): AddUserResult {
        runCatching {
            DSL.using(DbConnection.get())
                .transaction { config ->
                    val userResult = config.dsl()
                        .insertInto(users, users.USER_NAME)
                        .values(userName)
                        .returningResult(users)
                        .fetchOne()!!
                        .value1()!!

                    val passwordExtendData = config.dsl()
                        .insertInto(
                            userPasswordExtendData,
                            userPasswordExtendData.USER_ID,
                            userPasswordExtendData.ALGORITHM,
                            userPasswordExtendData.SALT,
                            userPasswordExtendData.ITERATION_COUNT,
                            userPasswordExtendData.KEY_LENGTH,
                        )
                        .values(
                            userResult.userId!!,
                            "PBKDF2WithHmacSHA512",
                            ByteArray(32).also {
                                SecureRandom().nextBytes(it)
                            },
                            100000,
                            512,
                        )
                        .returningResult(userPasswordExtendData)
                        .fetchOne()!!
                        .value1()!!

                    val spec: KeySpec = PBEKeySpec(
                        password.plus(ServerEnv.userPasswordPepper).toCharArray(),
                        passwordExtendData.salt!!,
                        passwordExtendData.iterationCount!!,
                        passwordExtendData.keyLength!!,
                    )
                    val factory = SecretKeyFactory.getInstance(passwordExtendData.algorithm!!)
                    val hashedPassword = bases64Encoder.encodeToString(factory.generateSecret(spec).encoded)

                    config.dsl()
                        .insertInto(
                            userPasswords,
                            userPasswords.USER_ID,
                            userPasswords.PASSWORD_HASH,
                        )
                        .values(
                            passwordExtendData.userId!!,
                            hashedPassword,
                        )
                        .execute()
                }
        }
            .onFailure { e ->
                return AddUserResult.Failed(
                    AddUserResult.ErrorType.InternalServerError(e),
                )
            }
        return AddUserResult.Success
    }

    sealed interface AddUserResult {
        object Success : AddUserResult
        data class Failed(val error: ErrorType) : AddUserResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }
}
