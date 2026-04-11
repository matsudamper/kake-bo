package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecordInput

public class NotificationUsageRepositoryAndroidImplTest {
    @Test
    public fun `未追加は parser に一致した通知だけ出る`() = runBlocking {
        val dao = FakeNotificationUsageDao(
            initialEntities = listOf(
                NotificationUsageEntity(
                    notificationKey = "matched",
                    packageName = "com.example",
                    text = "match text",
                    postedAtEpochMillis = 20,
                    receivedAtEpochMillis = 20,
                    isAdded = false,
                ),
                NotificationUsageEntity(
                    notificationKey = "unmatched",
                    packageName = "com.other",
                    text = "other text",
                    postedAtEpochMillis = 10,
                    receivedAtEpochMillis = 10,
                    isAdded = false,
                ),
            ),
        )
        val repository = NotificationUsageRepositoryAndroidImpl(
            dao = dao,
            parsers = listOf(ComExampleParser()),
        )

        val matched = repository.unaddedMatchedNotificationsFlow().first()
        val all = repository.notificationsFlow().first()
        val notAdded = repository.notAddedNotificationsFlow().first()
        val added = repository.addedNotificationsFlow().first()
        val unmatchedDetail = repository.notificationDetailFlow("unmatched").first()

        assertEquals(listOf("matched"), matched.map { it.record.notificationKey })
        assertEquals(listOf("matched", "unmatched"), all.map { it.notificationKey })
        assertEquals(listOf("matched", "unmatched"), notAdded.map { it.notificationKey })
        assertEquals("match text", matched.single().draft.description)
        assertEquals("com.example", matched.single().filterDefinition.id)
        assertEquals(listOf(), added.map { it.notificationKey })
        assertEquals(false, matched.single().record.isAdded)
        assertEquals(null, unmatchedDetail?.matched)
    }

    @Test
    public fun `通知は受信時刻の降順で出る`() = runBlocking {
        val dao = FakeNotificationUsageDao(
            initialEntities = listOf(
                NotificationUsageEntity(
                    notificationKey = "old-not-added",
                    packageName = "com.example",
                    text = "old text",
                    postedAtEpochMillis = 100,
                    receivedAtEpochMillis = 10,
                    isAdded = false,
                ),
                NotificationUsageEntity(
                    notificationKey = "added",
                    packageName = "com.example",
                    text = "added text",
                    postedAtEpochMillis = 20,
                    receivedAtEpochMillis = 20,
                    isAdded = true,
                ),
                NotificationUsageEntity(
                    notificationKey = "new-not-added",
                    packageName = "com.example",
                    text = "new text",
                    postedAtEpochMillis = 5,
                    receivedAtEpochMillis = 30,
                    isAdded = false,
                ),
            ),
        )
        val repository = NotificationUsageRepositoryAndroidImpl(
            dao = dao,
            parsers = listOf(ComExampleParser()),
        )

        val matched = repository.unaddedMatchedNotificationsFlow().first()
        val all = repository.notificationsFlow().first()
        val notAdded = repository.notAddedNotificationsFlow().first()
        val added = repository.addedNotificationsFlow().first()

        assertEquals(listOf("new-not-added", "old-not-added"), matched.map { it.record.notificationKey })
        assertEquals(listOf("new-not-added", "added", "old-not-added"), all.map { it.notificationKey })
        assertEquals(listOf("new-not-added", "old-not-added"), notAdded.map { it.notificationKey })
        assertEquals(listOf("added"), added.map { it.notificationKey })
    }

    @Test
    public fun `追加済みに更新できる`() = runBlocking {
        val dao = FakeNotificationUsageDao(
            initialEntities = listOf(
                NotificationUsageEntity(
                    notificationKey = "matched",
                    packageName = "com.example",
                    text = "match text",
                    postedAtEpochMillis = 20,
                    receivedAtEpochMillis = 20,
                    isAdded = false,
                ),
            ),
        )
        val repository = NotificationUsageRepositoryAndroidImpl(
            dao = dao,
            parsers = listOf(ComExampleParser()),
        )

        repository.markNotificationAsAdded("matched", MoneyUsageId(10))

        val matched = repository.unaddedMatchedNotificationsFlow().first()
        val added = repository.addedNotificationsFlow().first()
        assertEquals(listOf(), matched.map { it.record.notificationKey })
        assertEquals(true, added.single().isAdded)
        assertEquals(MoneyUsageId(10), added.single().moneyUsageId)
    }

    @Test
    public fun `同じ通知の再取り込みでも追加済み状態を保持する`() = runBlocking {
        val dao = FakeNotificationUsageDao(
            initialEntities = listOf(
                NotificationUsageEntity(
                    notificationKey = "matched",
                    packageName = "com.example",
                    text = "old text",
                    postedAtEpochMillis = 20,
                    receivedAtEpochMillis = 20,
                    isAdded = true,
                    moneyUsageId = 10,
                ),
            ),
        )
        val repository = NotificationUsageRepositoryAndroidImpl(
            dao = dao,
            parsers = listOf(ComExampleParser()),
        )

        repository.upsertNotification(
            NotificationUsageRecordInput(
                notificationKey = "matched",
                packageName = "com.example",
                text = "new text",
                postedAtEpochMillis = 30,
                receivedAtEpochMillis = 30,
            ),
        )

        val detail = repository.notificationDetailFlow("matched").first()
        assertEquals(true, detail?.record?.isAdded)
        assertEquals(MoneyUsageId(10), detail?.record?.moneyUsageId)
        assertEquals("new text", detail?.record?.text)
    }

    private class FakeNotificationUsageDao(
        initialEntities: List<NotificationUsageEntity>,
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

        override suspend fun upsert(entity: NotificationUsageEntity) {
            entitiesFlow.value = entitiesFlow.value
                .filterNot { it.notificationKey == entity.notificationKey } + entity
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

        private fun List<NotificationUsageEntity>.sortByReceivedAtDescending(): List<NotificationUsageEntity> {
            return sortedWith(
                compareByDescending<NotificationUsageEntity> { it.receivedAtEpochMillis }
                    .thenByDescending { it.postedAtEpochMillis },
            )
        }
    }

    private class ComExampleParser : NotificationUsageParser {
        override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
            id = "com.example",
            title = "com.example",
            description = "packageName が com.example の通知の全文を description に入れる",
        )

        override fun parse(record: net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord): NotificationUsageDraft? {
            return if (record.packageName == "com.example") {
                NotificationUsageDraft(description = record.text)
            } else {
                null
            }
        }
    }
}
