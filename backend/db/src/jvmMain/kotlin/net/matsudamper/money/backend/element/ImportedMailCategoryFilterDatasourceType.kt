package net.matsudamper.money.backend.element

enum class ImportedMailCategoryFilterDatasourceType(internal val dbValue: Int) {
    MailTitle(0),
    MailFrom(1),
    MailBody(2),
    Title(3),
    ServiceName(4),
    ;

    companion object {
        fun fromDbValue(dbValue: Int): ImportedMailCategoryFilterDatasourceType {
            return values().first { it.dbValue == dbValue }
        }
    }
}
