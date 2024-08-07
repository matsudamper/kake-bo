package net.matsudamper.money.backend.graphql.converter

import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.graphql.model.QlImportedMailFilterCategoryConditionOperator

internal fun QlImportedMailFilterCategoryConditionOperator.toDBElement(): ImportedMailFilterCategoryConditionOperator {
    return when (this) {
        QlImportedMailFilterCategoryConditionOperator.OR -> ImportedMailFilterCategoryConditionOperator.OR
        QlImportedMailFilterCategoryConditionOperator.AND -> ImportedMailFilterCategoryConditionOperator.AND
    }
}
