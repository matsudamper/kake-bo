package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser

public class NotificationUsageRepositoryAndroidImplTest {
    @Test
    public fun `parser に一致した通知だけ matched に出る`() = runBlocking {
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

        val matched = repository.matchedNotificationsFlow().first()
        val unmatched = repository.unmatchedNotificationsFlow().first()

        assertEquals(listOf("matched"), matched.map { it.record.notificationKey })
        assertEquals("match text", matched.single().draft.description)
        assertEquals(listOf("unmatched"), unmatched.map { it.notificationKey })
        assertEquals(false, matched.single().record.isAdded)
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

        repository.markNotificationAsAdded("matched")

        val matched = repository.matchedNotificationsFlow().first()
        assertEquals(true, matched.single().record.isAdded)
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
                ),
            ),
        )
        val repository = NotificationUsageRepositoryAndroidImpl(
            dao = dao,
            parsers = listOf(ComExampleParser()),
        )

        repository.upsertNotification(
            net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecordInput(
                notificationKey = "matched",
                packageName = "com.example",
                text = "new text",
                postedAtEpochMillis = 30,
                receivedAtEpochMillis = 30,
            ),
        )

        val matched = repository.matchedNotificationsFlow().first()
        assertEquals(true, matched.single().record.isAdded)
        assertEquals("new text", matched.single().record.text)
    }

    private class FakeNotificationUsageDao(
        initialEntities: List<NotificationUsageEntity>,
    ) : NotificationUsageDao {
        private val entitiesFlow = MutableStateFlow(initialEntities)

        override fun observeAll(): Flow<List<NotificationUsageEntity>> = entitiesFlow

        override suspend fun upsert(entity: NotificationUsageEntity) {
            entitiesFlow.value = entitiesFlow.value
                .filterNot { it.notificationKey == entity.notificationKey } + entity
        }

        override suspend fun findByKey(notificationKey: String): NotificationUsageEntity? {
            return entitiesFlow.value.firstOrNull { it.notificationKey == notificationKey }
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
    }

    private class ComExampleParser : NotificationUsageParser {
        override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
            id = "com.example",
            title = "com.example",
            matchDescription = "packageName が com.example",
            parseDescription = "description に全文を入れる",
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
