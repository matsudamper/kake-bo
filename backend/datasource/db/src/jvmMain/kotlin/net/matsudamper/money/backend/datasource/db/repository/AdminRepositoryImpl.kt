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

    override fun searchUsers(query: String, size: Int, cursor: String?): AdminRepository.SearchUsersResult {
        return runCatching {
            DbConnectionImpl.use {
                val fetchSize = size + 1
                val condition = if (cursor != null) {
                    users.USER_NAME.contains(query).and(users.USER_NAME.gt(cursor))
                } else {
                    users.USER_NAME.contains(query)
                }
                val records = DSL.using(it)
                    .select(users.USER_NAME)
                    .from(users)
                    .where(condition)
                    .orderBy(users.USER_NAME)
                    .limit(fetchSize)
                    .fetch()
                    .map { record -> record.value1()!! }

                val hasMore = records.size > size
                val result = if (hasMore) records.dropLast(1) else records
                AdminRepository.SearchUsersResult(
                    users = result,
                    cursor = result.lastOrNull(),
                    hasMore = hasMore,
                )
            }
        }.getOrElse {
            AdminRepository.SearchUsersResult(
                users = listOf(),
                cursor = null,
                hasMore = false,
            )
        }
    }

    override fun replacePassword(
        userName: String,
        hashedPassword: String,
        algorithmName: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
    ): AdminRepository.ReplacePasswordResult {
        return try {
            DbConnectionImpl.use { connection ->
                val userId = DSL.using(connection)
                    .select(users.USER_ID)
                    .from(users)
                    .where(users.USER_NAME.eq(userName))
                    .fetchOne()
                    ?.value1()

                if (userId == null) {
                    return@use AdminRepository.ReplacePasswordResult.UserNotFound
                }

                DSL.using(connection)
                    .transaction { config ->
                        config.dsl()
                            .update(userPasswords)
                            .set(userPasswords.PASSWORD_HASH, hashedPassword)
                            .where(userPasswords.USER_ID.eq(userId))
                            .execute()

                        config.dsl()
                            .update(userPasswordExtendData)
                            .set(userPasswordExtendData.ALGORITHM, algorithmName)
                            .set(userPasswordExtendData.SALT, salt)
                            .set(userPasswordExtendData.ITERATION_COUNT, iterationCount)
                            .set(userPasswordExtendData.KEY_LENGTH, keyLength)
                            .where(userPasswordExtendData.USER_ID.eq(userId))
                            .execute()
                    }

                AdminRepository.ReplacePasswordResult.Success
            }
        } catch (e: Exception) {
            AdminRepository.ReplacePasswordResult.Failed(
                AdminRepository.ReplacePasswordResult.ErrorType.InternalServerError(e),
            )
        }
    }
}
