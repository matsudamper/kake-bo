package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.AppSettingsRepository
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

public class NotificationUsageAutoAddProcessorTest : DescribeSpec(
    {
        describe("通知利用履歴の自動追加") {
            it("自動追加が OFF の時は追加しない") {
                val dao = AutoAddFakeNotificationUsageDao(
                    NotificationUsageEntity(
                        notificationKey = "key",
                        packageName = "com.example",
                        text = "body",
                        postedAtEpochMillis = 10,
                        receivedAtEpochMillis = 20,
                        isAdded = false,
                    ),
                )
                val api = AutoAddFakeNotificationUsageAutoAddApi()
                val processor = NotificationUsageAutoAddProcessor(
                    dao = dao,
                    parsers = listOf(AutoAddComExampleParser()),
                    appSettingsRepository = AutoAddFakeAppSettingsRepository(
                        autoAddEnabledByFilterId = mapOf(
                            "com.example" to false,
                        ),
                    ),
                    api = api,
                )

                processor.process("key")

                api.payloads.size.shouldBe(0)
                dao.findByKey("key")?.isAdded.shouldBe(false)
            }

            it("自動追加が ON の時は補完値で追加して追加済みにする") {
                val entity = NotificationUsageEntity(
                    notificationKey = "key",
                    packageName = "com.example",
                    text = "body",
                    postedAtEpochMillis = 1_000,
                    receivedAtEpochMillis = 2_000,
                    isAdded = false,
                )
                val dao = AutoAddFakeNotificationUsageDao(entity)
                val api = AutoAddFakeNotificationUsageAutoAddApi()
                val processor = NotificationUsageAutoAddProcessor(
                    dao = dao,
                    parsers = listOf(AutoAddComExampleParser()),
                    appSettingsRepository = AutoAddFakeAppSettingsRepository(
                        autoAddEnabledByFilterId = mapOf(
                            "com.example" to true,
                        ),
                    ),
                    api = api,
                )

                processor.process("key")

                api.payloads.size.shouldBe(1)
                api.payloads.single().shouldBe(
                    NotificationUsageAutoAddPayload(
                        title = "com.example",
                        description = "body",
                        amount = 0,
                        dateTime = Instant.fromEpochMilliseconds(entity.postedAtEpochMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()),
                        subCategoryId = null,
                    ),
                )
                dao.findByKey("key")?.isAdded.shouldBe(true)
                dao.findByKey("key")?.moneyUsageId.shouldBe(10)
            }

            it("自動追加済みの通知は再実行しない") {
                val dao = AutoAddFakeNotificationUsageDao(
                    NotificationUsageEntity(
                        notificationKey = "key",
                        packageName = "com.example",
                        text = "body",
                        postedAtEpochMillis = 10,
                        receivedAtEpochMillis = 20,
                        isAdded = true,
                    ),
                )
                val api = AutoAddFakeNotificationUsageAutoAddApi()
                val processor = NotificationUsageAutoAddProcessor(
                    dao = dao,
                    parsers = listOf(AutoAddComExampleParser()),
                    appSettingsRepository = AutoAddFakeAppSettingsRepository(
                        autoAddEnabledByFilterId = mapOf(
                            "com.example" to true,
                        ),
                    ),
                    api = api,
                )

                processor.process("key")

                api.payloads.size.shouldBe(0)
            }
        }
    },
)

private class AutoAddFakeAppSettingsRepository(
    autoAddEnabledByFilterId: Map<String, Boolean>,
) : AppSettingsRepository {
    override val showImagesInMonthlyScreen: Flow<Boolean> = MutableStateFlow(false)
    private val notificationUsageAutoAddEnabledFlows = autoAddEnabledByFilterId
        .mapValuesTo(mutableMapOf()) { (_, enabled) -> MutableStateFlow(enabled) }

    override fun setShowImagesInMonthlyScreen(value: Boolean) {
    }

    override fun notificationUsageAutoAddEnabled(filterId: String): Flow<Boolean> {
        return notificationUsageAutoAddEnabledFlows.getOrPut(filterId) { MutableStateFlow(false) }
    }

    override fun setNotificationUsageAutoAddEnabled(filterId: String, value: Boolean) {
        notificationUsageAutoAddEnabledFlows.getOrPut(filterId) { MutableStateFlow(value) }.value = value
    }
}

private class AutoAddFakeNotificationUsageDao(
    entity: NotificationUsageEntity,
) : NotificationUsageDao {
    private val entities = mutableMapOf(entity.notificationKey to entity)

    override fun observeAll(): Flow<List<NotificationUsageEntity>> = MutableStateFlow(entities.values.toList())

    override fun observeNotAdded(): Flow<List<NotificationUsageEntity>> {
        return MutableStateFlow(entities.values.toList()).map { entities ->
            entities.filter { it.isAdded.not() }
        }
    }

    override fun observeAdded(): Flow<List<NotificationUsageEntity>> {
        return MutableStateFlow(entities.values.toList()).map { entities ->
            entities.filter { it.isAdded }
        }
    }

    override suspend fun insert(entity: NotificationUsageEntity) {
        if (entity.notificationKey !in entities) {
            entities[entity.notificationKey] = entity
        }
    }

    override suspend fun findByKey(notificationKey: String): NotificationUsageEntity? {
        return entities[notificationKey]
    }

    override fun observeByKey(notificationKey: String): Flow<NotificationUsageEntity?> {
        return MutableStateFlow(entities[notificationKey])
    }

    override suspend fun markAsAdded(notificationKey: String, moneyUsageId: Int?) {
        entities[notificationKey] = entities[notificationKey]?.copy(
            isAdded = true,
            moneyUsageId = moneyUsageId,
        ) ?: return
    }
}

private class AutoAddFakeNotificationUsageAutoAddApi : NotificationUsageAutoAddApi {
    val payloads: MutableList<NotificationUsageAutoAddPayload> = mutableListOf()

    override suspend fun addUsage(payload: NotificationUsageAutoAddPayload): MoneyUsageId? {
        payloads += payload
        return MoneyUsageId(10)
    }
}

private class AutoAddComExampleParser : NotificationUsageParser {
    override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
        id = "com.example",
        title = "com.example",
        description = "packageName が com.example の通知の全文を description に入れる",
    )

    override fun parse(record: NotificationUsageRecord): NotificationUsageDraft? {
        return if (record.packageName == "com.example") {
            NotificationUsageDraft()
        } else {
            null
        }
    }
}
