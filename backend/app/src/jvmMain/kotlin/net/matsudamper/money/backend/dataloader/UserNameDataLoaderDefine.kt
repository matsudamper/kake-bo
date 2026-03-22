package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.UserRepository
import net.matsudamper.money.element.UserId

class UserNameDataLoaderDefine(
    private val userRepository: UserRepository,
) : DataLoaderDefine<UserId, String> {
    override val key: String = this::class.java.name

    override fun load(keys: Set<UserId>): Map<UserId, String> {
        val results = userRepository.getUserName(keys.toList())
        return keys.associateWith {
            results[it] ?: throw IllegalStateException("not result key: $key")
        }
    }
}
