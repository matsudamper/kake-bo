package net.matsudamper.money.frontend.common.base.navigator

import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array

public object CredentialModel {
    public suspend fun create(
        userId: Long,
        name: String,
        type: Type,
        challenge: String,
        domain: String,
    ): CredentialsContainerCreateResult {
        val id = Uint8Array(userId.toString().encodeToByteArray().toTypedArray())
        val options = CredentialsContainerCreateOptions(
            publicKey = CredentialsContainerCreatePublicKeyOptions(
                challenge = Uint8Array(challenge.encodeToByteArray().toTypedArray()),
                user = CredentialsContainerCreatePublicKeyOptions.User(
                    id = id,
                    name = name,
                    displayName = name,
                ),
                pubKeyCredParams = arrayOf(
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -7), // ES256
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -257), // RS256
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -8), // Ed25519
                ),
                excludeCredentials = arrayOf(),
                authenticatorSelection = CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection(
                    authenticatorAttachment = when (type) {
                        Type.PLATFORM -> CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection.AUTH_TYPE_PLATFORM
                        Type.CROSS_PLATFORM -> CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection.AUTH_TYPE_CROSS_PLATFORM
                    },
                    userVerification = "required",
                    residentKey = "required",
                ),
                rp = CredentialsContainerCreatePublicKeyOptions.Rp(
                    name = domain,
                    id = domain,
                ),
            ),
        )
        val result = navigator.credentials.create(
            options,
        ).await()
        return result
    }

    public enum class Type {
        PLATFORM,
        CROSS_PLATFORM,
    }
}
