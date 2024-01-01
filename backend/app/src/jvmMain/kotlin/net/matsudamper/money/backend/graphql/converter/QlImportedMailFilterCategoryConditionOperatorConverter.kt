package net.matsudamper.money.backend.graphql.converter

import net.matsudamper.money.backend.datasource.db.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.graphql.model.QlImportedMailFilterCategoryConditionOperator

internal fun QlImportedMailFilterCategoryConditionOperator.toDBElement(): ImportedMailFilterCategoryConditionOperator {
    return when (this) {
        QlImportedMailFilterCategoryConditionOperator.OR -> ImportedMailFilterCategoryConditionOperator.OR
        QlImportedMailFilterCategoryConditionOperator.AND -> ImportedMailFilterCategoryConditionOperator.AND
    }
}
