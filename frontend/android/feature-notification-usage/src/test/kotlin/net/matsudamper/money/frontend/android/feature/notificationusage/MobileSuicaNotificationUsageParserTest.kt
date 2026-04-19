package net.matsudamper.money.frontend.android.feature.notificationusage

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

public class MobileSuicaNotificationUsageParserTest : DescribeSpec(
    {
        val parser = MobileSuicaNotificationUsageParser()

        describe("モバイルSuicaパーサー") {
            it("通常の利用金額を正しく解析する") {
                val record = createRecord(
                    "モバイルSuica\n-555円\n残高: 2,000円",
                )
                val result = parser.parse(record)
                result.shouldNotBe(null)
                result!!.amount.shouldBe(555)
            }

            it("カンマ区切りの利用金額を正しく解析する") {
                val record = createRecord(
                    "モバイルSuica\n-1,234円\n残高: 10,000円",
                )
                val result = parser.parse(record)
                result.shouldNotBe(null)
                result!!.amount.shouldBe(1234)
            }

            it("支払いなしの場合はnullを返す") {
                val record = createRecord(
                    "モバイルSuica\n支払いなし\n残高: 2,555円",
                )
                val result = parser.parse(record)
                result.shouldBe(null)
            }

            it("チャージ（プラスの金額）の場合はnullを返す") {
                val record = createRecord(
                    "モバイルSuica\n+1,000円\n残高: 3,555円",
                )
                val result = parser.parse(record)
                result.shouldBe(null)
            }

            it("パッケージ名が異なる場合はnullを返す") {
                val record = createRecord(
                    text = "モバイルSuica\n-555円",
                    packageName = "com.other.app",
                )
                val result = parser.parse(record)
                result.shouldBe(null)
            }

            it("タイトルが「モバイルSuica」でない場合はnullを返す") {
                val record = createRecord(
                    "Suica\n-555円",
                )
                val result = parser.parse(record)
                result.shouldBe(null)
            }
        }
    },
)

private fun createRecord(
    text: String,
    packageName: String = "com.felicanetworks.mfm.main",
): NotificationUsageRecord {
    return NotificationUsageRecord(
        notificationKey = "key",
        packageName = packageName,
        text = text,
        postedAtEpochMillis = 1_000,
        receivedAtEpochMillis = 1_000,
    )
}
