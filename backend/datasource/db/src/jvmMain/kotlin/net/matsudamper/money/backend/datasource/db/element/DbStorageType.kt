package net.matsudamper.money.backend.datasource.db.element

internal enum class DbStorageType(val dbValue: String) {
    LOCAL("LOCAL"),
    S3("S3"),
}
