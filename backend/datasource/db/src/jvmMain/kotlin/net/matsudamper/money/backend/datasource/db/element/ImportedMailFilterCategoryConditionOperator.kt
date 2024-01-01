package net.matsudamper.money.backend.datasource.db.element

enum class ImportedMailFilterCategoryConditionOperator(internal val dbValue: Int) {
    AND(1),
    OR(0),
    ;

    companion object {
        internal fun fromDbValue(dbValue: Int): ImportedMailFilterCategoryConditionOperator {
            return values().first { it.dbValue == dbValue }
        }
    }
}
