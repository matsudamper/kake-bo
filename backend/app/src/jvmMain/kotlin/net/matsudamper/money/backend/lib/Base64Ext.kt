package net.matsudamper.money.backend.lib

import java.util.Base64

internal fun String.encodeBase64(): String {
    return Base64.getEncoder().encodeToString(toByteArray())
}

internal fun String.decodeBase64String(): String {
    return Base64.getDecoder().decode(this).toString(Charsets.UTF_8)
}

internal fun String.decodeBase64(): ByteArray {
    return Base64.getDecoder().decode(this)
}

internal fun ByteArray.encodeBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}
