package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.lib.flatten

class MoneyUsageAssociateByImportedMailDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<MoneyUsageAssociateByImportedMailDataLoaderDefine.Key, MoneyUsageAssociateByImportedMailDataLoaderDefine.Result> {
    override val key: String = this::class.java.name

    override fun load(keys: Set<Key>): Map<Key, Result> {
        val repository = repositoryFactory.createDbMailRepository()

        val results = keys.groupBy { it.userId }
            .mapNotNull { (userId, key) ->
                val result = repository
                    .getMails(
                        userId = userId,
                        moneyUsageIdList = key.map { it.moneyUsageId },
                    ).onFailure {
                        it.printStackTrace()
                    }.getOrNull() ?: return mapOf()

                result.map { (usageId, mailId) ->
                    Key(
                        userId = userId,
                        moneyUsageId = usageId,
                    ) to mailId
                }.toMap()
            }.flatten()

        return keys.associateWith { key ->
            val result = results[key].orEmpty()
            Result(
                userId = key.userId,
                mailIdList = result,
            )
        }
    }

    data class Result(
        val userId: UserId,
        val mailIdList: List<ImportedMailId>,
    )

    data class Key(
        val userId: UserId,
        val moneyUsageId: MoneyUsageId,
    )
}
