package net.matsudamper.money.backend.feature.objectstorage

public data class ObjectStorageConfig(
    val endpoint: String,
    val region: String,
    val bucket: String,
    val roleArn: String,
    val roleSessionName: String,
    val audience: String,
    val pathStyleAccess: Boolean,
)
