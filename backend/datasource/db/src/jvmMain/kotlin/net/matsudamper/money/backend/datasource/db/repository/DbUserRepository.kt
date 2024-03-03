package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.UserRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JUsers
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbUserRepository : UserRepository {
    private val user = JUsers.USERS

    override fun getUserName(userIdList: List<UserId>): Map<UserId, String> {
        return DbConnectionImpl.use {
            DSL.using(it)
                .select(user.USER_ID, user.USER_NAME)
                .from(user)
                .where(user.USER_ID.`in`(userIdList.map { userId -> userId.value }))
                .fetch()
                .associate { record ->
                    UserId(record.getValue<Int>(user.USER_ID)) to record.getValue<String>(user.USER_NAME)
                }
        }
    }

    override fun getUserId(userName: String): UserId? {
        return DbConnectionImpl.use {
            val userId =
                DSL.using(it)
                    .select(user.USER_ID)
                    .from(user)
                    .where(user.USER_NAME.eq(userName))
                    .fetchOne()
                    ?.getValue<Int>(user.USER_ID)
                    ?: return@use null
            UserId(userId)
        }
    }
}
