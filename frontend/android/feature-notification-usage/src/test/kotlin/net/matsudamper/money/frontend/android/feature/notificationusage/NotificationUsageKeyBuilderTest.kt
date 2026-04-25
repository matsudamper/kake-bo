package net.matsudamper.money.frontend.android.feature.notificationusage

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

public class NotificationUsageKeyBuilderTest : DescribeSpec(
    {
        describe("通知 key の生成") {
            it("同じ通知値は同じ key になる") {
                val first = NotificationUsageKeyBuilder.build(
                    notificationKey = "android-key",
                    packageName = "com.example",
                    text = "100円",
                    postedAtEpochMillis = 1_000,
                )
                val second = NotificationUsageKeyBuilder.build(
                    notificationKey = "android-key",
                    packageName = "com.example",
                    text = "100円",
                    postedAtEpochMillis = 1_000,
                )

                first.shouldBe(second)
            }

            it("通知値が変わると別の key になる") {
                val before = NotificationUsageKeyBuilder.build(
                    notificationKey = "android-key",
                    packageName = "com.example",
                    text = "100円",
                    postedAtEpochMillis = 1_000,
                )
                val after = NotificationUsageKeyBuilder.build(
                    notificationKey = "android-key",
                    packageName = "com.example",
                    text = "200円",
                    postedAtEpochMillis = 1_000,
                )

                before.shouldNotBe(after)
            }
        }
    },
)
