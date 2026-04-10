package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

public class ComExampleNotificationUsageParserTest {
    private val parser = ComExampleNotificationUsageParser()

    @Test
    public fun `filter 情報を返す`() {
        assertEquals("com.example", parser.filterDefinition.id)
        assertEquals("com.example サンプル", parser.filterDefinition.title)
    }

    @Test
    public fun `com example の通知だけ一致する`() {
        val result = parser.parse(
            NotificationUsageRecord(
                notificationKey = "key",
                packageName = "com.example",
                text = "sample body",
                postedAtEpochMillis = 1,
                receivedAtEpochMillis = 2,
                isAdded = false,
            ),
        )

        assertEquals("sample body", result?.description)
    }

    @Test
    public fun `対象外 package は一致しない`() {
        val result = parser.parse(
            NotificationUsageRecord(
                notificationKey = "key",
                packageName = "com.other",
                text = "sample body",
                postedAtEpochMillis = 1,
                receivedAtEpochMillis = 2,
                isAdded = false,
            ),
        )

        assertNull(result)
    }
}
