package net.matsudamper.money.backend.fido

data class Base64FidoAuthenticator(
    val base64CredentialId: String,
    val base64AttestationStatement: String,
    val attestationStatementFormat: String,
    val base64AttestedCredentialData: String,
    val counter: Long,
)
