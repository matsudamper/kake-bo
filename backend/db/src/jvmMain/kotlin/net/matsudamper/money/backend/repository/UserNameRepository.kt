package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnectionImpl
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JUsers
import org.jooq.impl.DSL

class UserNameRepository {
    private val user = JUsers.USERS

    fun getUserName(userIdList: List<UserId>): Map<UserId, String> {
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
}
