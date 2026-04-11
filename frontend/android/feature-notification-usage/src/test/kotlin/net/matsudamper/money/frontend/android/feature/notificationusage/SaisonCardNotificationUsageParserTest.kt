package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

public class SaisonCardNotificationUsageParserTest {
    private val parser = SaisonCardNotificationUsageParser()

    @Test
    public fun `filter 情報を返す`() {
        assertEquals("jp.co.saisoncard.android.saisonportal", parser.filterDefinition.id)
        assertEquals("セゾンカード", parser.filterDefinition.title)
    }

    @Test
    public fun `カードのご利用通知をパースできる`() {
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

        assertEquals("DMM.com", result?.title)
        assertEquals(3111, result?.amount)
        assertEquals(
            LocalDateTime(
                date = LocalDate(2026, 4, 11),
                time = LocalTime(12, 33),
            ),
            result?.dateTime,
        )
    }

    @Test
    public fun `対象外 package は一致しない`() {
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

        assertNull(result)
    }
}
