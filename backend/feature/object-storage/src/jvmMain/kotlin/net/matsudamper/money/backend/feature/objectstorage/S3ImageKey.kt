package net.matsudamper.money.backend.feature.objectstorage

internal object S3ImageKey {
    fun of(relativePath: String): String = "img/$relativePath"
}
