package net.matsudamper.money.frontend.common.base.navigator

import kotlinx.coroutines.await
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

public object WebAuthModel {
    public suspend fun create(
        id: String,
        name: String,
        type: Type,
        challenge: String,
        domain: String,
        base64ExcludeCredentialIdList: List<String>,
    ): CreateResult? {
        val options =
            createOption(
                userId = id,
                name = name,
                type = type,
                challenge = challenge,
                domain = domain,
                excludeCredentials =
                    base64ExcludeCredentialIdList.map {
                        CredentialsContainerCreatePublicKeyOptions.ExcludeCredential(
                            id = it.decodeBase64Bytes(),
                            type = "public-key",
                        )
                    },
            )
        val result =
            runCatching {
                navigator.credentials.create(
                    options,
                ).await()
            }.onFailure {
                it.printStackTrace()
            }.getOrNull() ?: return null

        val attestationObjectBase64 = result.response.attestationObject.toBase64()
        val clientDataJSONBase64 = result.response.clientDataJSON.toBase64()

        return CreateResult(
            attestationObjectBase64 = attestationObjectBase64,
            clientDataJSONBase64 = clientDataJSONBase64,
        )
    }

    public suspend fun get(
        userId: String,
        name: String,
        type: Type,
        challenge: String,
        domain: String,
    ): GetResult? {
        val options =
            createOption(
                userId = userId,
                name = name,
                type = type,
                challenge = challenge,
                domain = domain,
                excludeCredentials = emptyList(),
            )

        val result =
            runCatching {
                navigator.credentials.get(
                    options,
                ).await()
            }.onFailure {
                it.printStackTrace()
            }.getOrNull() ?: return null
        console.log(result)
        return GetResult(
            credentialId = result.id,
            base64ClientDataJSON = result.response.clientDataJSON.toBase64(),
            base64Signature = result.response.signature.toBase64(),
            base64UserHandle = result.response.userHandle.toBase64(),
            base64AuthenticatorData = result.response.authenticatorData.toBase64(),
        )
    }

    private fun createOption(
        userId: String,
        name: String,
        type: Type,
        challenge: String,
        domain: String,
        excludeCredentials: List<CredentialsContainerCreatePublicKeyOptions.ExcludeCredential>,
    ): CredentialsContainerCreateOptions {
        val id = Uint8Array(userId.encodeToByteArray().toTypedArray())
        return CredentialsContainerCreateOptions(
            publicKey =
                CredentialsContainerCreatePublicKeyOptions(
                    challenge = Uint8Array(challenge.encodeToByteArray().toTypedArray()),
                    user =
                        CredentialsContainerCreatePublicKeyOptions.User(
                            id = id,
                            name = name,
                            displayName = name,
                        ),
                    pubKeyCredParams =
                        arrayOf(
                            CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -7), // ES256
                            CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -257), // RS256
                            CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -8), // Ed25519
                        ),
                    excludeCredentials = excludeCredentials.toTypedArray(),
                    authenticatorSelection =
                        CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection(
                            authenticatorAttachment =
                                when (type) {
                                    Type.PLATFORM -> CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection.AUTH_TYPE_PLATFORM
                                    Type.CROSS_PLATFORM -> CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection.AUTH_TYPE_CROSS_PLATFORM
                                },
                            userVerification = "required",
                            residentKey = "required",
                        ),
                    rp =
                        CredentialsContainerCreatePublicKeyOptions.Rp(
                            name = domain,
                            id = domain,
                        ),
                ),
        )
    }

    public data class CreateResult(
        val attestationObjectBase64: String,
        val clientDataJSONBase64: String,
    )

    public data class GetResult(
        val base64AuthenticatorData: String,
        val base64ClientDataJSON: String,
        val base64Signature: String,
        val base64UserHandle: String,
        val credentialId: String,
    )

    private fun ArrayBuffer.toBase64(): String {
        return buildList {
            val uint8Array = Uint8Array(this@toBase64)
            for (index in 0 until this@toBase64.byteLength) {
                add(uint8Array[index])
            }
        }.toByteArray().encodeBase64()
    }

    public enum class Type {
        PLATFORM,
        CROSS_PLATFORM,
    }
}
