package net.matsudamper.money.backend.fido

import java.util.Base64
import com.webauthn4j.converter.AttestedCredentialDataConverter
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.credential.CredentialRecord
import com.webauthn4j.credential.CredentialRecordImpl

object AuthenticatorConverter {
    private val objectConverter = ObjectConverter()
    private val attestedCredentialDataConverter = AttestedCredentialDataConverter(objectConverter)

    private val base64Encoder = Base64.getEncoder()
    private val base64Decoder = Base64.getDecoder()

    internal fun convertToBase64(credentialRecord: CredentialRecord): Base64FidoAuthenticator {
        val attestationStatement = credentialRecord.attestationStatement
            ?: throw IllegalStateException("attestationStatement is null. credentialRecord=$credentialRecord")

        return Base64FidoAuthenticator(
            base64AttestationStatement = base64Encoder.encodeToString(
                objectConverter.cborMapper.writeValueAsBytes(attestationStatement),
            ),
            attestationStatementFormat = attestationStatement.format,
            base64AttestedCredentialData = base64Encoder.encodeToString(
                attestedCredentialDataConverter.convert(credentialRecord.attestedCredentialData),
            ),
            counter = credentialRecord.counter,
            base64CredentialId = base64Encoder.encodeToString(credentialRecord.attestedCredentialData.credentialId),
        )
    }

    fun convertFromBase64(
        @Suppress("UNUSED_PARAMETER") base64AttestationStatement: String,
        @Suppress("UNUSED_PARAMETER") attestationStatementFormat: String,
        base64AttestedCredentialData: String,
        counter: Long,
    ): FidoAuthenticatorWrapper {
        val credentialRecord = CredentialRecordImpl(
            // 検証しない
            // https://www.w3.org/TR/webauthn-1/#sctn-no-attestation-security-attestation
            // https://developers.yubico.com/WebAuthn/WebAuthn_Developer_Guide/Attestation.html
            null,
            null,
            null,
            null,
            counter,
            attestedCredentialDataConverter.convert(
                base64Decoder.decode(base64AttestedCredentialData),
            ),
            null,
            null,
            null,
            null,
        )
        return FidoAuthenticatorWrapper(
            credentialRecord = credentialRecord,
        )
    }
}

class FidoAuthenticatorWrapper(
    internal val credentialRecord: CredentialRecord,
) {
    val credentialId: ByteArray = credentialRecord.attestedCredentialData.credentialId
    val counter: Long = credentialRecord.counter
}
