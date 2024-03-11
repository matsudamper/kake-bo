package net.matsudamper.money.backend.graphql.resolver.user

import java.time.LocalDateTime
import net.matsudamper.money.backend.lib.CursorParser
import net.matsudamper.money.element.MoneyUsageId

internal class MoneyUsagesCursor(
    val lastId: MoneyUsageId,
    val lastDate: LocalDateTime,
) {
    fun toCursorString(): String {
        return CursorParser.createToString(
            mapOf(
                LAST_ID_KEY to lastId.id.toString(),
                LAST_DATE_KEY to lastDate.toString(),
            ),
        )
    }

    companion object {
        private const val LAST_ID_KEY = "lastId"
        private const val LAST_DATE_KEY = "lastDate"

        fun fromString(cursorString: String): MoneyUsagesCursor {
            return MoneyUsagesCursor(
                lastId =
                MoneyUsageId(
                    CursorParser.parseFromString(cursorString)[LAST_ID_KEY]!!.toInt(),
                ),
                lastDate =
                LocalDateTime.parse(
                    CursorParser.parseFromString(cursorString)[LAST_DATE_KEY]!!,
                ),
            )
        }
    }
}
