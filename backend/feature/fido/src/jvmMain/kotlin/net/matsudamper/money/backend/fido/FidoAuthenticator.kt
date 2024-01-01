package net.matsudamper.money.backend.fido

data class FidoAuthenticator(
    val attestationStatement: String,
    val attestationStatementFormat: String,
    val attestedCredentialData: String,
    val counter: Long,
)
