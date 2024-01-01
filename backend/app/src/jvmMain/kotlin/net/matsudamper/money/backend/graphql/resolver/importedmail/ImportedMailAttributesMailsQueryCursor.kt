package net.matsudamper.money.backend.graphql.resolver.importedmail

import java.time.LocalDateTime
import net.matsudamper.money.backend.lib.CursorParser
import net.matsudamper.money.backend.datasource.db.repository.DbMailRepository
import net.matsudamper.money.element.ImportedMailId

internal data class ImportedMailAttributesMailsQueryCursor(
    val pagingInfo: DbMailRepository.PagingInfo,
) {
    fun toCursorString(): String {
        val map = when (pagingInfo) {
            is DbMailRepository.PagingInfo.CreatedDateTime -> {
                mapOf(
                    LAST_MAIL_ID_KEY to pagingInfo.importedMailId.id.toString(),
                    CREATED_DATETIME_KEY to pagingInfo.time.toString(),
                    TYPE_KEY to Type.CREATE_DATETIME.typeValue,
                )
            }

            is DbMailRepository.PagingInfo.DateTime -> {
                mapOf(
                    LAST_MAIL_ID_KEY to pagingInfo.importedMailId.id.toString(),
                    DATETIME_KEY to pagingInfo.time.toString(),
                    TYPE_KEY to Type.DATETIME.typeValue,
                )
            }
        }

        return CursorParser.createToString(map)
    }

    companion object {
        private const val LAST_MAIL_ID_KEY = "lastMailId"
        private const val CREATED_DATETIME_KEY = "created_datetime"
        private const val DATETIME_KEY = "datetime"
        private const val TYPE_KEY = "type_key"

        enum class Type(val typeValue: String) {
            CREATE_DATETIME("CREATE_DATETIME"),
            DATETIME("DATETIME"),
            ;

            companion object {
                fun valueOfTypeValue(value: String): Type {
                    return values().first { it.typeValue == value }
                }
            }
        }

        fun fromString(value: String): ImportedMailAttributesMailsQueryCursor {
            val parseResult = CursorParser.parseFromString(value)
            val mailId = ImportedMailId(parseResult[LAST_MAIL_ID_KEY]!!.toInt())

            return ImportedMailAttributesMailsQueryCursor(
                when (Type.valueOfTypeValue(parseResult[TYPE_KEY]!!)) {
                    Type.CREATE_DATETIME -> {
                        DbMailRepository.PagingInfo.CreatedDateTime(
                            importedMailId = mailId,
                            time = LocalDateTime.parse(parseResult[CREATED_DATETIME_KEY]!!),
                        )
                    }

                    Type.DATETIME -> {
                        DbMailRepository.PagingInfo.DateTime(
                            importedMailId = mailId,
                            time = LocalDateTime.parse(parseResult[DATETIME_KEY]!!),
                        )
                    }
                },
            )
        }
    }
}
