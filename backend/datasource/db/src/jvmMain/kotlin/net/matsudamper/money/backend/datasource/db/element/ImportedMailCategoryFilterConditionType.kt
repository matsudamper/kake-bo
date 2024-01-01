package net.matsudamper.money.backend.datasource.db.element

enum class ImportedMailCategoryFilterConditionType(internal val dbValue: Int) {
    Include(0),
    NotInclude(1),
    Equal(2),
    NotEqual(3),
    ;

    companion object {
        fun fromDbValue(dbValue: Int): ImportedMailCategoryFilterConditionType {
            return ImportedMailCategoryFilterConditionType.values()
                .first { it.dbValue == dbValue }
        }
    }
}
