package net.matsudamper.money.backend.datasource.db.element

import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator

internal enum class DbImportedMailFilterCategoryConditionOperator(val dbValue: Int) {
    AND(1),
    OR(0),
    ;

    fun toLogicValue(): ImportedMailFilterCategoryConditionOperator {
        return when (this) {
            AND -> ImportedMailFilterCategoryConditionOperator.AND
            OR -> ImportedMailFilterCategoryConditionOperator.OR
        }
    }

    companion object {
        fun fromDbValue(dbValue: Int): DbImportedMailFilterCategoryConditionOperator {
            return entries.first { it.dbValue == dbValue }
        }
    }
}

internal fun ImportedMailFilterCategoryConditionOperator.toDbDefine(): DbImportedMailFilterCategoryConditionOperator {
    return when (this) {
        ImportedMailFilterCategoryConditionOperator.AND -> DbImportedMailFilterCategoryConditionOperator.AND
        ImportedMailFilterCategoryConditionOperator.OR -> DbImportedMailFilterCategoryConditionOperator.OR
    }
}
