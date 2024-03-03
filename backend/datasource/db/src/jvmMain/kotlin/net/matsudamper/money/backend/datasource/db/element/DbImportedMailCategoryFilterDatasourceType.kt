package net.matsudamper.money.backend.datasource.db.element

import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterDatasourceType

enum class DbImportedMailCategoryFilterDatasourceType(internal val dbValue: Int) {
    MailTitle(0),
    MailFrom(1),
    MailHTML(2),
    Title(3),
    ServiceName(4),
    MailPlain(5),
    ;

    fun toLogicValue(): ImportedMailCategoryFilterDatasourceType {
        return when (this) {
            MailTitle -> ImportedMailCategoryFilterDatasourceType.MailTitle
            MailFrom -> ImportedMailCategoryFilterDatasourceType.MailFrom
            MailHTML -> ImportedMailCategoryFilterDatasourceType.MailHTML
            Title -> ImportedMailCategoryFilterDatasourceType.Title
            ServiceName -> ImportedMailCategoryFilterDatasourceType.ServiceName
            MailPlain -> ImportedMailCategoryFilterDatasourceType.MailPlain
        }
    }

    companion object {
        fun fromDbValue(dbValue: Int): DbImportedMailCategoryFilterDatasourceType {
            return entries.first { it.dbValue == dbValue }
        }
    }
}

internal fun ImportedMailCategoryFilterDatasourceType.toDbDefine(): DbImportedMailCategoryFilterDatasourceType {
    return when (this) {
        ImportedMailCategoryFilterDatasourceType.MailTitle -> DbImportedMailCategoryFilterDatasourceType.MailTitle
        ImportedMailCategoryFilterDatasourceType.MailFrom -> DbImportedMailCategoryFilterDatasourceType.MailFrom
        ImportedMailCategoryFilterDatasourceType.MailHTML -> DbImportedMailCategoryFilterDatasourceType.MailHTML
        ImportedMailCategoryFilterDatasourceType.Title -> DbImportedMailCategoryFilterDatasourceType.Title
        ImportedMailCategoryFilterDatasourceType.ServiceName -> DbImportedMailCategoryFilterDatasourceType.ServiceName
        ImportedMailCategoryFilterDatasourceType.MailPlain -> DbImportedMailCategoryFilterDatasourceType.MailPlain
    }
}
