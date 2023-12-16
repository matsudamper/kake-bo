package net.matsudamper.money.frontend.common.base.navigator

import kotlinx.coroutines.await
import io.ktor.util.encodeBase64
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

public object CredentialModel {
    public suspend fun create(
        userId: Long,
        name: String,
        type: Type,
        challenge: String,
        domain: String,
    ): CreateResult {
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

        val attestationObjectBase64 = buildList {
            val uint8Array = Uint8Array(result.response.attestationObject)
            for (index in 0 until result.response.attestationObject.byteLength) {
                add(uint8Array[index])
            }
        }.toByteArray().encodeBase64()
        val clientDataJSONBase64 = buildList {
            val uint8Array = Uint8Array(result.response.clientDataJSON)
            for (index in 0 until result.response.clientDataJSON.byteLength) {
                add(uint8Array[index])
            }
        }.toByteArray().encodeBase64()

        return CreateResult(
            attestationObjectBase64 = attestationObjectBase64,
            clientDataJSONBase64 = clientDataJSONBase64,
        )
    }

    public data class CreateResult(
        val attestationObjectBase64: String,
        val clientDataJSONBase64: String,
    )

    public enum class Type {
        PLATFORM,
        CROSS_PLATFORM,
    }
}
