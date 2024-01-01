package net.matsudamper.money.backend.datasource.db.element

enum class ImportedMailCategoryFilterDatasourceType(internal val dbValue: Int) {
    MailTitle(0),
    MailFrom(1),
    MailHTML(2),
    Title(3),
    ServiceName(4),
    MailPlain(5),
    ;

    companion object {
        fun fromDbValue(dbValue: Int): ImportedMailCategoryFilterDatasourceType {
            return values().first { it.dbValue == dbValue }
        }
    }
}
