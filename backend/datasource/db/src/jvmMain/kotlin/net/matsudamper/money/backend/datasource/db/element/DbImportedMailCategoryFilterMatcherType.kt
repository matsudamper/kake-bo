package net.matsudamper.money.backend.datasource.db.element

import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterMatcherType

internal enum class DbImportedMailCategoryFilterMatcherType(val dbValue: Int) {
    Include(0),
    NotInclude(1),
    Equal(2),
    NotEqual(3),
    ;

    fun toLogicValue(): ImportedMailCategoryFilterMatcherType {
        return when (this) {
            Include -> ImportedMailCategoryFilterMatcherType.Include
            NotInclude -> ImportedMailCategoryFilterMatcherType.NotInclude
            Equal -> ImportedMailCategoryFilterMatcherType.Equal
            NotEqual -> ImportedMailCategoryFilterMatcherType.NotEqual
        }
    }

    companion object {
        fun fromDbValue(dbValue: Int): DbImportedMailCategoryFilterMatcherType {
            return DbImportedMailCategoryFilterMatcherType.entries
                .first { it.dbValue == dbValue }
        }
    }
}

internal fun ImportedMailCategoryFilterMatcherType.toDbDefine(): DbImportedMailCategoryFilterMatcherType {
    return when (this) {
        ImportedMailCategoryFilterMatcherType.Include -> DbImportedMailCategoryFilterMatcherType.Include
        ImportedMailCategoryFilterMatcherType.NotInclude -> DbImportedMailCategoryFilterMatcherType.NotInclude
        ImportedMailCategoryFilterMatcherType.Equal -> DbImportedMailCategoryFilterMatcherType.Equal
        ImportedMailCategoryFilterMatcherType.NotEqual -> DbImportedMailCategoryFilterMatcherType.NotEqual
    }
}
