package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

public class SaisonCardNotificationUsageParserTest : DescribeSpec(
    {
        val parser = SaisonCardNotificationUsageParser()

        describe("セゾンカード通知の解析") {
            it("filter 情報を返す") {
                parser.filterDefinition.id.shouldBe("jp.co.saisoncard.android.saisonportal")
                parser.filterDefinition.title.shouldBe("セゾンカード")
            }

            it("カードのご利用通知をパースできる") {
                val result = parser.parse(
                    NotificationUsageRecord(
                        notificationKey = "key",
                        packageName = "jp.co.saisoncard.android.saisonportal",
                        text = "カードのご利用がありました\n金額：3,111円\n場所：DMM.com\n日時：2026年4月11日 12時33分",
                        postedAtEpochMillis = 1,
                        receivedAtEpochMillis = 2,
                        isAdded = false,
                    ),
                )

                result?.title.shouldBe("DMM.com")
                result?.amount.shouldBe(3111)
                result?.dateTime.shouldBe(
                    LocalDateTime(
                        date = LocalDate(2026, 4, 11),
                        time = LocalTime(12, 33),
                    ),
                )
            }

            it("対象外 package は一致しない") {
                val result = parser.parse(
                    NotificationUsageRecord(
                        notificationKey = "key",
                        packageName = "com.other",
                        text = "カードのご利用がありました\n金額：3,111円\n場所：DMM.com\n日時：2026年4月11日 12時33分",
                        postedAtEpochMillis = 1,
                        receivedAtEpochMillis = 2,
                        isAdded = false,
                    ),
                )

                result.shouldBe(null)
            }
        }
    },
)
