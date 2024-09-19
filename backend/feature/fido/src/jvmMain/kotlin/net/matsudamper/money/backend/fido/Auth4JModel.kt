package net.matsudamper.money.backend.fido

import java.util.Base64
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.authenticator.Authenticator
import com.webauthn4j.authenticator.AuthenticatorImpl
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.AuthenticationRequest
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialType
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.RegistrationRequest
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.verifier.exception.VerificationException
import net.matsudamper.money.backend.base.ServerEnv

class Auth4JModel(
    challenge: String,
) {
    private val decoder = Base64.getDecoder()
    private val serverProperty =
        ServerProperty(
            setOf(
                Origin("https://${ServerEnv.domain!!}"),
                // https://passkeys-auth.com/docs/implementation/flutter/android/#origin
                Origin("android:apk-key-hash:${ServerEnv.apkKeyHash}"),
            ),
            ServerEnv.domain!!,
            { challenge.toByteArray() },
            null,
        )

    fun register(
        base64AttestationObject: ByteArray,
        base64ClientDataJSON: ByteArray,
        clientExtensionsJSON: String?,
    ): Base64FidoAuthenticator {
        val webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager()

        val validatedData =
            webAuthnManager.validate(
                RegistrationRequest(
                    decoder.decode(base64AttestationObject),
                    decoder.decode(base64ClientDataJSON),
                    clientExtensionsJSON,
                ),
                RegistrationParameters(
                    serverProperty,
                    listOf(
                        PublicKeyCredentialParameters(
                            PublicKeyCredentialType.PUBLIC_KEY,
                            COSEAlgorithmIdentifier.ES256,
                        ),
                        PublicKeyCredentialParameters(
                            PublicKeyCredentialType.PUBLIC_KEY,
                            COSEAlgorithmIdentifier.RS256,
                        ),
                        PublicKeyCredentialParameters(
                            PublicKeyCredentialType.PUBLIC_KEY,
                            COSEAlgorithmIdentifier.EdDSA,
                        ),
                    ),
                    true,
                    false,
                ),
            )
        val attestationObject =
            validatedData.attestationObject
                ?: throw IllegalStateException("attestationObject is null. validatedData=$validatedData")
        val authenticator =
            AuthenticatorImpl(
                attestationObject.authenticatorData.attestedCredentialData!!,
                attestationObject.attestationStatement,
                attestationObject.authenticatorData.signCount,
            )

        return AuthenticatorConverter.convertToBase64(authenticator)
    }

    fun verify(
        authenticator: FidoAuthenticatorWrapper,
        credentialId: ByteArray,
        base64UserHandle: ByteArray,
        base64AuthenticatorData: ByteArray,
        base64ClientDataJSON: ByteArray,
        clientExtensionJSON: String?,
        base64Signature: ByteArray,
    ) {
        return verify(
            authenticator = authenticator.authenticator,
            request =
            AuthenticationRequest(
                credentialId,
                decoder.decode(base64UserHandle),
                decoder.decode(base64AuthenticatorData),
                decoder.decode(base64ClientDataJSON),
                clientExtensionJSON,
                decoder.decode(base64Signature),
            ),
        )
    }

    private fun verify(
        authenticator: Authenticator,
        request: AuthenticationRequest,
    ) {
        val webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager()

        runCatching {
            webAuthnManager.validate(
                webAuthnManager.parse(request),
                AuthenticationParameters(
                    serverProperty,
                    authenticator,
                    null,
                    true,
                    false,
                ),
            )
        }.onFailure {
            when (it) {
                is VerificationException -> Unit
                else -> it.printStackTrace()
            }
        }.getOrThrow()
    }
}
