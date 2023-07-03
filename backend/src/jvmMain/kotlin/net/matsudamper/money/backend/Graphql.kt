package net.matsudamper.money.backend

import com.fasterxml.jackson.annotation.JsonProperty

data class GraphQlRequest(
    @JsonProperty("query") val query: String? = null,
    @JsonProperty("operationName") val operationName: String = "",
    @JsonProperty("variables") val variables: Map<String, Any> = mapOf(),
    @JsonProperty("extensions") val extensions: Extensions? = null,
) {
    data class Extensions(
        @JsonProperty("persistedQuery") val persistedQuery: PersistedQuery? = null,
    )

    data class PersistedQuery(
        @JsonProperty("version") val version: String,
        @JsonProperty("sha256Hash") val sha256Hash: String,
    )
}
