package net.matsudamper.money.backend.base.lib

import java.util.Base64

public fun String.encodeBase64(): String {
    return Base64.getEncoder().encodeToString(toByteArray())
}

public fun String.decodeBase64String(): String {
    return Base64.getDecoder().decode(this).toString(Charsets.UTF_8)
}

public fun String.decodeBase64(): ByteArray {
    return Base64.getDecoder().decode(this)
}

public fun ByteArray.encodeBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}
