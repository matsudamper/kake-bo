package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.UserId

interface UserRepository {
    fun getUserName(userIdList: List<UserId>): Map<UserId, String>

    fun getUserId(userName: String): UserId?
}
