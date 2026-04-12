package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class FakeNotificationUsageDao(
    initialEntities: List<NotificationUsageEntity> = listOf(),
) : NotificationUsageDao {
    private val entitiesFlow = MutableStateFlow(initialEntities)
    private val linkedUsagesFlow = MutableStateFlow<List<NotificationUsageLinkedUsageEntity>>(emptyList())
    private var linkedUsageIdCounter = 1L

    override fun observeAll(): Flow<List<NotificationUsageEntity>> {
        return entitiesFlow.map { entities ->
            entities.sortByReceivedAtDescending()
        }
    }

    override fun observeNotAdded(): Flow<List<NotificationUsageEntity>> {
        return entitiesFlow.map { entities ->
            entities.filter { it.isAdded.not() }
                .sortByReceivedAtDescending()
        }
    }

    override fun observeAdded(): Flow<List<NotificationUsageEntity>> {
        return entitiesFlow.map { entities ->
            entities.filter { it.isAdded }
                .sortByReceivedAtDescending()
        }
    }

    override suspend fun insert(entity: NotificationUsageEntity) {
        if (entitiesFlow.value.none { it.notificationKey == entity.notificationKey }) {
            entitiesFlow.value += entity
        }
    }

    override suspend fun findByKey(notificationKey: String): NotificationUsageEntity? {
        return entitiesFlow.value.firstOrNull { it.notificationKey == notificationKey }
    }

    override fun observeByKey(notificationKey: String): Flow<NotificationUsageEntity?> {
        return entitiesFlow.map { entities ->
            entities.firstOrNull { it.notificationKey == notificationKey }
        }
    }

    override suspend fun markAsAdded(notificationKey: String) {
        entitiesFlow.value = entitiesFlow.value.map { entity ->
            if (entity.notificationKey == notificationKey) {
                entity.copy(isAdded = true)
            } else {
                entity
            }
        }
    }

    override suspend fun insertLinkedUsage(entity: NotificationUsageLinkedUsageEntity) {
        linkedUsagesFlow.value += entity.copy(id = linkedUsageIdCounter++)
    }

    override fun observeLinkedUsages(notificationKey: String): Flow<List<NotificationUsageLinkedUsageEntity>> {
        return combine(entitiesFlow, linkedUsagesFlow) { entities, linkedUsages ->
            val entityMoneyUsageId = entities
                .firstOrNull { it.notificationKey == notificationKey }
                ?.moneyUsageId
            val fromLinkedTable = linkedUsages.filter { it.notificationKey == notificationKey }
            val linkedIds = fromLinkedTable.map { it.moneyUsageId }.toSet()
            val legacyEntry = if (entityMoneyUsageId != null && entityMoneyUsageId !in linkedIds) {
                listOf(
                    NotificationUsageLinkedUsageEntity(
                        id = 0,
                        notificationKey = notificationKey,
                        moneyUsageId = entityMoneyUsageId,
                    ),
                )
            } else {
                emptyList()
            }
            legacyEntry + fromLinkedTable
        }
    }

    override suspend fun getLinkedUsages(notificationKey: String): List<NotificationUsageLinkedUsageEntity> {
        val entityMoneyUsageId = entitiesFlow.value
            .firstOrNull { it.notificationKey == notificationKey }
            ?.moneyUsageId
        val fromLinkedTable = linkedUsagesFlow.value.filter { it.notificationKey == notificationKey }
        val linkedIds = fromLinkedTable.map { it.moneyUsageId }.toSet()
        val legacyEntry = if (entityMoneyUsageId != null && entityMoneyUsageId !in linkedIds) {
            listOf(
                NotificationUsageLinkedUsageEntity(
                    id = 0,
                    notificationKey = notificationKey,
                    moneyUsageId = entityMoneyUsageId,
                ),
            )
        } else {
            emptyList()
        }
        return legacyEntry + fromLinkedTable
    }

    private fun List<NotificationUsageEntity>.sortByReceivedAtDescending(): List<NotificationUsageEntity> {
        return sortedWith(
            compareByDescending<NotificationUsageEntity> { it.receivedAtEpochMillis }
                .thenByDescending { it.postedAtEpochMillis },
        )
    }
}
