package net.matsudamper.money.frontend.graphql

public data class ServerHostConfig(
    val protocol: String,
    val defaultHost: String,
    val savedHost: String,
)
