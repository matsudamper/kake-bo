package net.matsudamper.money.backend.graphql.converter

import net.matsudamper.money.backend.element.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterConditionType

public fun QlImportedMailCategoryFilterConditionType.toDbElement(): ImportedMailCategoryFilterConditionType {
    return when(this) {
        QlImportedMailCategoryFilterConditionType.Include -> ImportedMailCategoryFilterConditionType.Include
        QlImportedMailCategoryFilterConditionType.NotInclude -> ImportedMailCategoryFilterConditionType.NotInclude
        QlImportedMailCategoryFilterConditionType.Equal -> ImportedMailCategoryFilterConditionType.Equal
        QlImportedMailCategoryFilterConditionType.NotEqual -> ImportedMailCategoryFilterConditionType.NotEqual
    }
}