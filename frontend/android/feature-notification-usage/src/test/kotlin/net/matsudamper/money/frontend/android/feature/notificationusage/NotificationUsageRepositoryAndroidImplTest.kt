package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.coroutines.flow.first
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecordInput

public class NotificationUsageRepositoryAndroidImplTest : DescribeSpec(
    {
        describe("通知利用履歴 repository") {
            it("未追加は parser に一致した通知だけ出る") {
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
                    parsers = listOf(RepositoryComExampleParser()),
                )

                val matched = repository.unaddedMatchedNotificationsFlow().first()
                val all = repository.notificationsFlow().first()
                val notAdded = repository.notAddedNotificationsFlow().first()
                val added = repository.addedNotificationsFlow().first()
                val unmatchedDetail = repository.notificationDetailFlow("unmatched").first()

                matched.map { it.record.notificationKey }.shouldBe(listOf("matched"))
                all.map { it.notificationKey }.shouldBe(listOf("matched", "unmatched"))
                notAdded.map { it.notificationKey }.shouldBe(listOf("matched", "unmatched"))
                matched.single().draft.description.shouldBe("match text")
                matched.single().filterDefinition.id.shouldBe("com.example")
                added.map { it.notificationKey }.shouldBe(listOf())
                matched.single().record.isAdded.shouldBe(false)
                unmatchedDetail?.matched.shouldBe(null)
            }

            it("通知は受信時刻の降順で出る") {
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
                    parsers = listOf(RepositoryComExampleParser()),
                )

                val matched = repository.unaddedMatchedNotificationsFlow().first()
                val all = repository.notificationsFlow().first()
                val notAdded = repository.notAddedNotificationsFlow().first()
                val added = repository.addedNotificationsFlow().first()

                matched.map { it.record.notificationKey }.shouldBe(listOf("new-not-added", "old-not-added"))
                all.map { it.notificationKey }.shouldBe(listOf("new-not-added", "added", "old-not-added"))
                notAdded.map { it.notificationKey }.shouldBe(listOf("new-not-added", "old-not-added"))
                added.map { it.notificationKey }.shouldBe(listOf("added"))
            }

            it("追加済みに更新できる") {
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
                    parsers = listOf(RepositoryComExampleParser()),
                )

                repository.markNotificationAsAdded("matched", MoneyUsageId(10))

                val matched = repository.unaddedMatchedNotificationsFlow().first()
                val added = repository.addedNotificationsFlow().first()
                matched.map { it.record.notificationKey }.shouldBe(listOf())
                added.single().isAdded.shouldBe(true)

                val detail = repository.notificationDetailFlow("matched").first()
                detail?.record?.moneyUsageIds.shouldBe(listOf(MoneyUsageId(10)))
            }

            it("値が変わった同じ Android 通知は別通知として保存する") {
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
                    parsers = listOf(RepositoryComExampleParser()),
                )

                val storedKey = repository.upsertNotification(
                    NotificationUsageRecordInput(
                        notificationKey = "matched",
                        packageName = "com.example",
                        text = "new text",
                        postedAtEpochMillis = 30,
                        receivedAtEpochMillis = 30,
                    ),
                )

                val detail = repository.notificationDetailFlow("matched").first()
                val newDetail = repository.notificationDetailFlow(storedKey).first()
                val all = repository.notificationsFlow().first()
                storedKey.shouldNotBe("matched")
                all.map { it.notificationKey }.shouldBe(listOf(storedKey, "matched"))
                detail?.record?.isAdded.shouldBe(true)
                detail?.record?.moneyUsageIds.shouldBe(listOf(MoneyUsageId(10)))
                detail?.record?.text.shouldBe("old text")
                newDetail?.record?.isAdded.shouldBe(false)
                newDetail?.record?.text.shouldBe("new text")
            }

            it("値が同じ Android 通知の再取り込みでは保存済み通知を上書きしない") {
                val dao = FakeNotificationUsageDao(
                    initialEntities = listOf(
                        NotificationUsageEntity(
                            notificationKey = "matched",
                            packageName = "com.example",
                            text = "same text",
                            postedAtEpochMillis = 20,
                            receivedAtEpochMillis = 20,
                            isAdded = true,
                            moneyUsageId = 10,
                        ),
                    ),
                )
                val repository = NotificationUsageRepositoryAndroidImpl(
                    dao = dao,
                    parsers = listOf(RepositoryComExampleParser()),
                )

                val storedKey = repository.upsertNotification(
                    NotificationUsageRecordInput(
                        notificationKey = "matched",
                        packageName = "com.example",
                        text = "same text",
                        postedAtEpochMillis = 20,
                        receivedAtEpochMillis = 30,
                    ),
                )

                val all = repository.notificationsFlow().first()
                storedKey.shouldBe("matched")
                all.map { it.notificationKey }.shouldBe(listOf("matched"))
                all.single().isAdded.shouldBe(true)
                val detail = repository.notificationDetailFlow("matched").first()
                detail?.record?.moneyUsageIds.shouldBe(listOf(MoneyUsageId(10)))
                all.single().receivedAtEpochMillis.shouldBe(20L)
            }
        }
    },
)

private class RepositoryComExampleParser : NotificationUsageParser {
    override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
        id = "com.example",
        title = "com.example",
        description = "packageName が com.example の通知の全文を description に入れる",
    )

    override fun parse(record: NotificationUsageRecord): NotificationUsageDraft? {
        return if (record.packageName == "com.example") {
            NotificationUsageDraft(description = record.text)
        } else {
            null
        }
    }
}
