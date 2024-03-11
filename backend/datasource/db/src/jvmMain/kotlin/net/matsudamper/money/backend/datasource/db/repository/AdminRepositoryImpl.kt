package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.AdminRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JUserPasswordExtendData
import net.matsudamper.money.db.schema.tables.JUserPasswords
import net.matsudamper.money.db.schema.tables.JUsers
import org.jooq.impl.DSL

class AdminRepositoryImpl : AdminRepository {
    private val users = JUsers.USERS
    private val userPasswords = JUserPasswords.USER_PASSWORDS
    private val userPasswordExtendData = JUserPasswordExtendData.USER_PASSWORD_EXTEND_DATA

    override fun addUser(
        userName: String,
        hashedPassword: String,
        algorithmName: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
    ): AdminRepository.AddUserResult {
        runCatching {
            DbConnectionImpl.use {
                DSL.using(it)
                    .transaction { config ->
                        val userResult =
                            config.dsl()
                                .insertInto(users, users.USER_NAME)
                                .values(userName)
                                .returningResult(users)
                                .fetchOne()!!
                                .value1()!!

                        val passwordExtendData =
                            config.dsl()
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
                                    algorithmName,
                                    salt,
                                    iterationCount,
                                    keyLength,
                                )
                                .returningResult(userPasswordExtendData)
                                .fetchOne()!!
                                .value1()!!

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
