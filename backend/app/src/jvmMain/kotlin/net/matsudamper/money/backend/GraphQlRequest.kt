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
        @param:JsonProperty("clientLibrary") val clientLibrary: ClientLibrary? = null,
    )

    data class ClientLibrary(
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("version") val version: String,
    )

    data class PersistedQuery(
        @param:JsonProperty("version") val version: String,
        @param:JsonProperty("sha256Hash") val sha256Hash: String,
    )
}
