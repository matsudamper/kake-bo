package net.matsudamper.money.backend.fido

import java.util.Base64
import com.webauthn4j.authenticator.Authenticator
import com.webauthn4j.authenticator.AuthenticatorImpl
import com.webauthn4j.converter.AttestedCredentialDataConverter
import com.webauthn4j.converter.util.ObjectConverter

object AuthenticatorConverter {
    private val objectConverter = ObjectConverter()
    private val attestedCredentialDataConverter = AttestedCredentialDataConverter(objectConverter)

    private val base64Encoder = Base64.getEncoder()
    private val base64Decoder = Base64.getDecoder()

    internal fun convertToBase64(authenticator: Authenticator): Base64FidoAuthenticator {
        val attestationStatement =
            authenticator.attestationStatement
                ?: throw IllegalStateException("attestationStatement is null. authenticator=$authenticator")

        return Base64FidoAuthenticator(
            base64AttestationStatement =
            base64Encoder.encodeToString(
                objectConverter.cborConverter.writeValueAsBytes(attestationStatement),
            ),
            attestationStatementFormat = attestationStatement.format,
            base64AttestedCredentialData =
            base64Encoder.encodeToString(
                attestedCredentialDataConverter.convert(authenticator.attestedCredentialData),
            ),
            counter = authenticator.counter,
            base64CredentialId = base64Encoder.encodeToString(authenticator.attestedCredentialData.credentialId),
        )
    }

    fun convertFromBase64(
        @Suppress("UNUSED_PARAMETER") base64AttestationStatement: String,
        @Suppress("UNUSED_PARAMETER") attestationStatementFormat: String,
        base64AttestedCredentialData: String,
        counter: Long,
    ): FidoAuthenticatorWrapper {
        val authenticator =
            AuthenticatorImpl(
                attestedCredentialDataConverter.convert(
                    base64Decoder.decode(base64AttestedCredentialData),
                ),
                // 検証しない
                // https://www.w3.org/TR/webauthn-1/#sctn-no-attestation-security-attestation
                // https://developers.yubico.com/WebAuthn/WebAuthn_Developer_Guide/Attestation.html
                null,
                counter,
            )
        return FidoAuthenticatorWrapper(
            authenticator = authenticator,
        )
    }
}

class FidoAuthenticatorWrapper(
    internal val authenticator: Authenticator,
) {
    val credentialId: ByteArray = authenticator.attestedCredentialData.credentialId
    val counter: Long = authenticator.counter
}
