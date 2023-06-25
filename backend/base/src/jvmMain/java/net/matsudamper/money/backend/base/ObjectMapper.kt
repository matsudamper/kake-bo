package net.matsudamper.money.backend.base

import kotlinx.serialization.json.Json
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object ObjectMapper {
    val jackson = jacksonObjectMapper()
    val kotlinxSerialization = Json {
        prettyPrint = true
        ignoreUnknownKeys = true

        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }
}
