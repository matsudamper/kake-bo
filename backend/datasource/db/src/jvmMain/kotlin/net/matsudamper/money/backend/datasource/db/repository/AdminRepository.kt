package net.matsudamper.money.backend.datasource.db.repository

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import net.matsudamper.money.backend.app.interfaces.AdminRepository
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JUserPasswordExtendData
import net.matsudamper.money.db.schema.tables.JUserPasswords
import net.matsudamper.money.db.schema.tables.JUsers
import org.jooq.impl.DSL

class AdminRepositoryImpl : AdminRepository {
    private val users = JUsers.USERS
    private val userPasswords = JUserPasswords.USER_PASSWORDS
    private val userPasswordExtendData = JUserPasswordExtendData.USER_PASSWORD_EXTEND_DATA
    private val bases64Encoder = Base64.getEncoder()

    override fun addUser(userName: String, password: String): AdminRepository.AddUserResult {
        runCatching {
            DbConnectionImpl.use {
                DSL.using(it)
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
        }
            .onFailure { e ->
                return AdminRepository.AddUserResult.Failed(
                    AdminRepository.AddUserResult.ErrorType.InternalServerError(e),
                )
            }

        return AdminRepository.AddUserResult.Success
    }
}
