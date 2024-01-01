package net.matsudamper.money.backend.graphql.converter

import net.matsudamper.money.backend.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterDataSourceType

fun QlImportedMailCategoryFilterDataSourceType.toDbElement(): ImportedMailCategoryFilterDatasourceType {
    return when (this) {
        QlImportedMailCategoryFilterDataSourceType.MailTitle -> ImportedMailCategoryFilterDatasourceType.MailTitle
        QlImportedMailCategoryFilterDataSourceType.MailFrom -> ImportedMailCategoryFilterDatasourceType.MailFrom
        QlImportedMailCategoryFilterDataSourceType.MailHtml -> ImportedMailCategoryFilterDatasourceType.MailHTML
        QlImportedMailCategoryFilterDataSourceType.Title -> ImportedMailCategoryFilterDatasourceType.Title
        QlImportedMailCategoryFilterDataSourceType.ServiceName -> ImportedMailCategoryFilterDatasourceType.ServiceName
        QlImportedMailCategoryFilterDataSourceType.MailPlain -> ImportedMailCategoryFilterDatasourceType.MailPlain
    }
}
