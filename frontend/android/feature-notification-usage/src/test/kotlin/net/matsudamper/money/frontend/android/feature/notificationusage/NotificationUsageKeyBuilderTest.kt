package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

public class NotificationUsageKeyBuilderTest {
    @Test
    public fun `同じ通知値は同じ key になる`() {
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

        assertEquals(first, second)
    }

    @Test
    public fun `通知値が変わると別の key になる`() {
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

        assertNotEquals(before, after)
    }
}
