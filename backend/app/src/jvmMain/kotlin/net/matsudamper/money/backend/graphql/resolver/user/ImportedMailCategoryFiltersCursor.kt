package net.matsudamper.money.backend.graphql.resolver.user

import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.lib.CursorParser
import net.matsudamper.money.element.ImportedMailCategoryFilterId

internal class ImportedMailCategoryFiltersCursor(
    private val cursor: MailFilterRepository.MailFilterCursor,
) {
    fun toCursorString(): String {
        return CursorParser.createToString(
            mapOf(
                ID to cursor.id.id.toString(),
                TITLE to cursor.title,
                ORDER_NUM to cursor.orderNumber.toString(),
            ),
        )
    }

    companion object {
        private const val ID = "ID"
        private const val TITLE = "TITLE"
        private const val ORDER_NUM = "ORDER_NUM"

        fun fromString(cursorString: String): MailFilterRepository.MailFilterCursor {
            val map = CursorParser.parseFromString(cursorString)
            return MailFilterRepository.MailFilterCursor(
                id = ImportedMailCategoryFilterId(map[ID]!!.toInt()),
                title = map[TITLE]!!,
                orderNumber = map[ORDER_NUM]!!.toInt(),
            )
        }
    }
}
