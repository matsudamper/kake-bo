package net.matsudamper.money.backend.graphql.resolver.user

import java.time.LocalDateTime
import net.matsudamper.money.backend.lib.CursorParser
import net.matsudamper.money.element.MoneyUsageId

internal class MoneyUsagesCursor(
    val lastId: MoneyUsageId,
    val lastDate: LocalDateTime?,
    val amount: Int?,
    val lastCreatedDateTime: LocalDateTime?,
) {
    fun toCursorString(): String {
        return CursorParser.createToString(
            mapOf(
                LAST_ID_KEY to lastId.id.toString(),
                LAST_DATE_KEY to lastDate?.toString().orEmpty(),
                AMOUNT_KEY to amount?.toString().orEmpty(),
                LAST_CREATED_DATE_TIME_KEY to lastCreatedDateTime?.toString().orEmpty(),
            ),
        )
    }

    companion object {
        private const val LAST_ID_KEY = "lastId"
        private const val LAST_DATE_KEY = "lastDate"
        private const val AMOUNT_KEY = "amount"
        private const val LAST_CREATED_DATE_TIME_KEY = "lastCreatedDateTime"

        fun fromString(cursorString: String): MoneyUsagesCursor {
            return MoneyUsagesCursor(
                lastId = MoneyUsageId(
                    CursorParser.parseFromString(cursorString)[LAST_ID_KEY]!!.toInt(),
                ),
                lastDate = run date@{
                    val text = CursorParser.parseFromString(cursorString)[LAST_DATE_KEY]
                        ?.takeIf { it.isNotBlank() }
                        ?: return@date null
                    LocalDateTime.parse(text)
                },
                amount = CursorParser.parseFromString(cursorString)[AMOUNT_KEY]?.toIntOrNull(),
                lastCreatedDateTime = run createdDateTime@{
                    val text = CursorParser.parseFromString(cursorString)[LAST_CREATED_DATE_TIME_KEY]
                        ?.takeIf { it.isNotBlank() }
                        ?: return@createdDateTime null
                    LocalDateTime.parse(text)
                },
            )
        }
    }
}
