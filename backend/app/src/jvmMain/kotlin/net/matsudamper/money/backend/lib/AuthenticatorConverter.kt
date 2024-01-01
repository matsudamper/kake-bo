package net.matsudamper.money.backend.lib

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

    fun convertToBase64(authenticator: Authenticator): Base64Authenticator {
        val attestationStatement = authenticator.attestationStatement
            ?: throw IllegalStateException("attestationStatement is null. authenticator=$authenticator")

        return Base64Authenticator(
            base64AttestationStatement = base64Encoder.encodeToString(
                objectConverter.cborConverter.writeValueAsBytes(attestationStatement),
            ),
            attestationStatementFormat = attestationStatement.format,
            base64AttestedCredentialData = base64Encoder.encodeToString(
                attestedCredentialDataConverter.convert(authenticator.attestedCredentialData),
            ),
            counter = authenticator.counter,
        )
    }

    fun convertFromBase64(base64Authenticator: Base64Authenticator): Authenticator {
        return AuthenticatorImpl(
            attestedCredentialDataConverter.convert(
                base64Decoder.decode(base64Authenticator.base64AttestedCredentialData),
            ),
            // 検証しない
            // https://www.w3.org/TR/webauthn-1/#sctn-no-attestation-security-attestation
            // https://developers.yubico.com/WebAuthn/WebAuthn_Developer_Guide/Attestation.html
            null,
            base64Authenticator.counter,
        )
    }

    data class Base64Authenticator(
        val base64AttestationStatement: String,
        val attestationStatementFormat: String,
        val base64AttestedCredentialData: String,
        val counter: Long,
    )
}
