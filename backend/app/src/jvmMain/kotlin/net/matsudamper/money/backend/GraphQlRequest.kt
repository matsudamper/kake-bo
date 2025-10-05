package net.matsudamper.money.backend

import com.fasterxml.jackson.annotation.JsonProperty

data class GraphQlRequest(
    @param:JsonProperty("query") val query: String? = null,
    @param:JsonProperty("operationName") val operationName: String = "",
    @param:JsonProperty("variables") val variables: Map<String, Any> = mapOf(),
    @param:JsonProperty("extensions") val extensions: Extensions? = null,
) {
    data class Extensions(
        @param:JsonProperty("persistedQuery") val persistedQuery: PersistedQuery? = null,
    )

    data class PersistedQuery(
        @param:JsonProperty("version") val version: String,
        @param:JsonProperty("sha256Hash") val sha256Hash: String,
    )
}
