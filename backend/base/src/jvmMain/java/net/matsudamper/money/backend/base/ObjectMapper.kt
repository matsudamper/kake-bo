package net.matsudamper.money.backend.base

import kotlinx.serialization.json.Json
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

public object ObjectMapper {
    public val jackson: ObjectMapper = jacksonObjectMapper()
    public val kotlinxSerialization: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true

        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }
}
