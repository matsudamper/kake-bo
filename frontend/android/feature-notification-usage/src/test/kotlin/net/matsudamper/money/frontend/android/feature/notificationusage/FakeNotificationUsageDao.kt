package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeNotificationUsageDao(
    initialEntities: List<NotificationUsageEntity> = listOf(),
) : NotificationUsageDao {
    private val entitiesFlow = MutableStateFlow(initialEntities)

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

    override suspend fun markAsAdded(notificationKey: String, moneyUsageId: Int?) {
        entitiesFlow.value = entitiesFlow.value.map { entity ->
            if (entity.notificationKey == notificationKey) {
                entity.copy(
                    isAdded = true,
                    moneyUsageId = moneyUsageId,
                )
            } else {
                entity
            }
        }
    }

    override suspend fun deleteByKey(notificationKey: String) {
        entitiesFlow.value = entitiesFlow.value.filter { it.notificationKey != notificationKey }
    }

    private fun List<NotificationUsageEntity>.sortByReceivedAtDescending(): List<NotificationUsageEntity> {
        return sortedWith(
            compareByDescending<NotificationUsageEntity> { it.receivedAtEpochMillis }
                .thenByDescending { it.postedAtEpochMillis },
        )
    }
}
