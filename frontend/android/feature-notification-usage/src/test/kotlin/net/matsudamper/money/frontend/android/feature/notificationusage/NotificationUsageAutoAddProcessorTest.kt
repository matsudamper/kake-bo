package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.AppSettingsRepository
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageKey
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

public class NotificationUsageAutoAddProcessorTest : DescribeSpec(
    {
        describe("通知利用履歴の自動追加") {
            it("自動追加が OFF の時は追加しない") {
                val dao = FakeNotificationUsageDao(
                    listOf(
                        NotificationUsageEntity(
                            notificationKey = "key",
                            packageName = "com.example",
                            text = "body",
                            postedAtEpochMillis = 10,
                            receivedAtEpochMillis = 20,
                            isAdded = false,
                        ),
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
                    categoryFilterRepository = FakeNotificationUsageCategoryFilterRepository(null),
                )

                processor.process(NotificationUsageKey("key"))

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
                val dao = FakeNotificationUsageDao(listOf(entity))
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
                    categoryFilterRepository = FakeNotificationUsageCategoryFilterRepository(null),
                )

                processor.process(NotificationUsageKey("key"))

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
                val dao = FakeNotificationUsageDao(
                    listOf(
                        NotificationUsageEntity(
                            notificationKey = "key",
                            packageName = "com.example",
                            text = "body",
                            postedAtEpochMillis = 10,
                            receivedAtEpochMillis = 20,
                            isAdded = true,
                        ),
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
                    categoryFilterRepository = FakeNotificationUsageCategoryFilterRepository(null),
                )

                processor.process(NotificationUsageKey("key"))

                api.payloads.size.shouldBe(0)
            }

            it("カテゴリフィルターがマッチした時は subCategoryId が設定される") {
                val subCategoryId = MoneyUsageSubCategoryId(42)
                val entity = NotificationUsageEntity(
                    notificationKey = "key",
                    packageName = "com.example",
                    text = "body",
                    postedAtEpochMillis = 1_000,
                    receivedAtEpochMillis = 2_000,
                    isAdded = false,
                )
                val dao = FakeNotificationUsageDao(listOf(entity))
                val api = AutoAddFakeNotificationUsageAutoAddApi()
                val processor = NotificationUsageAutoAddProcessor(
                    dao = dao,
                    parsers = listOf(AutoAddComExampleParser()),
                    appSettingsRepository = AutoAddFakeAppSettingsRepository(
                        autoAddEnabledByFilterId = mapOf("com.example" to true),
                    ),
                    api = api,
                    categoryFilterRepository = FakeNotificationUsageCategoryFilterRepository(subCategoryId),
                )

                processor.process(NotificationUsageKey("key"))

                api.payloads.size.shouldBe(1)
                api.payloads.single().subCategoryId.shouldBe(subCategoryId)
            }

            it("カテゴリフィルターがマッチしない時は subCategoryId が null になる") {
                val entity = NotificationUsageEntity(
                    notificationKey = "key",
                    packageName = "com.example",
                    text = "body",
                    postedAtEpochMillis = 1_000,
                    receivedAtEpochMillis = 2_000,
                    isAdded = false,
                )
                val dao = FakeNotificationUsageDao(listOf(entity))
                val api = AutoAddFakeNotificationUsageAutoAddApi()
                val processor = NotificationUsageAutoAddProcessor(
                    dao = dao,
                    parsers = listOf(AutoAddComExampleParser()),
                    appSettingsRepository = AutoAddFakeAppSettingsRepository(
                        autoAddEnabledByFilterId = mapOf("com.example" to true),
                    ),
                    api = api,
                    categoryFilterRepository = FakeNotificationUsageCategoryFilterRepository(null),
                )

                processor.process(NotificationUsageKey("key"))

                api.payloads.size.shouldBe(1)
                api.payloads.single().subCategoryId.shouldBe(null)
            }
        }
    },
)

private class FakeNotificationUsageCategoryFilterRepository(
    private val result: MoneyUsageSubCategoryId?,
) : NotificationUsageCategoryFilterRepository {
    override suspend fun getMatchingSubCategoryId(title: String, serviceName: String): MoneyUsageSubCategoryId? {
        return result
    }
}

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
        if (record.packageName != "com.example") return null
        return NotificationUsageDraft(
            title = filterDefinition.title,
            description = record.text,
            amount = null,
            dateTime = Instant.fromEpochMilliseconds(record.postedAtEpochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }
}
