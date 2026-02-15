package net.matsudamper.money.backend.image

import java.io.File

internal data class ImageUploadConfig(
    val storageDirectory: File,
    val maxUploadBytes: Long,
)
