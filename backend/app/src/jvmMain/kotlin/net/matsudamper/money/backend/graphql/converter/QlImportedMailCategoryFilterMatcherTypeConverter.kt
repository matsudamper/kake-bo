package net.matsudamper.money.backend.graphql.converter

import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterMatcherType
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterMatcherType

public fun QlImportedMailCategoryFilterMatcherType.toDbElement(): ImportedMailCategoryFilterMatcherType {
    return when (this) {
        QlImportedMailCategoryFilterMatcherType.Include -> ImportedMailCategoryFilterMatcherType.Include
        QlImportedMailCategoryFilterMatcherType.NotInclude -> ImportedMailCategoryFilterMatcherType.NotInclude
        QlImportedMailCategoryFilterMatcherType.Equal -> ImportedMailCategoryFilterMatcherType.Equal
        QlImportedMailCategoryFilterMatcherType.NotEqual -> ImportedMailCategoryFilterMatcherType.NotEqual
    }
}
