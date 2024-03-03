package net.matsudamper.money.backend.datasource.db.element

import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterConditionType

internal enum class DbImportedMailCategoryFilterConditionType(val dbValue: Int) {
    Include(0),
    NotInclude(1),
    Equal(2),
    NotEqual(3),
    ;

    fun toLogicValue(): ImportedMailCategoryFilterConditionType {
        return when (this) {
            Include -> ImportedMailCategoryFilterConditionType.Include
            NotInclude -> ImportedMailCategoryFilterConditionType.NotInclude
            Equal -> ImportedMailCategoryFilterConditionType.Equal
            NotEqual -> ImportedMailCategoryFilterConditionType.NotEqual
        }
    }

    companion object {
        fun fromDbValue(dbValue: Int): DbImportedMailCategoryFilterConditionType {
            return DbImportedMailCategoryFilterConditionType.entries
                .first { it.dbValue == dbValue }
        }
    }
}

internal fun ImportedMailCategoryFilterConditionType.toDbDefine(): DbImportedMailCategoryFilterConditionType {
    return when (this) {
        ImportedMailCategoryFilterConditionType.Include -> DbImportedMailCategoryFilterConditionType.Include
        ImportedMailCategoryFilterConditionType.NotInclude -> DbImportedMailCategoryFilterConditionType.NotInclude
        ImportedMailCategoryFilterConditionType.Equal -> DbImportedMailCategoryFilterConditionType.Equal
        ImportedMailCategoryFilterConditionType.NotEqual -> DbImportedMailCategoryFilterConditionType.NotEqual
    }
}
