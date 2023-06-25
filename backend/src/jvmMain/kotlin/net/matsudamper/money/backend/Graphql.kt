package net.matsudamper.money.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GraphQlRequest(
    @SerialName("query") val query: String? = null,
    @SerialName("operationName") val operationName: String = "",
    @SerialName("variables") val variables: Map<String, String> = mapOf(),
    @SerialName("extensions") val extensions: Extensions? = null,
) {
    @Serializable
    data class Extensions(
        @SerialName("persistedQuery") val persistedQuery: PersistedQuery? = null,
    )

    @Serializable
    data class PersistedQuery(
        @SerialName("version") val version: String,
        @SerialName("sha256Hash") val sha256Hash: String,
    )
}
