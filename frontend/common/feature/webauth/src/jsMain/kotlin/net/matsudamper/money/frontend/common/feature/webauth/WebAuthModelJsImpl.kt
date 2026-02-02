package net.matsudamper.money.frontend.common.feature.webauth

import kotlinx.coroutines.await
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

public class WebAuthModelJsImpl : WebAuthModel {
    public override suspend fun create(
        id: String,
        name: String,
        type: WebAuthModel.WebAuthModelType,
        challenge: String,
        domain: String,
        base64ExcludeCredentialIdList: List<String>,
    ): WebAuthModel.WebAuthCreateResult? {
        val options = createOption(
            type = type,
            challenge = challenge,
            domain = domain,
            excludeCredentials = base64ExcludeCredentialIdList.map {
                CredentialsContainerCreatePublicKeyOptions.ExcludeCredential(
                    id = it.decodeBase64Bytes(),
                    type = "public-key",
                )
            },
        )
        val result = runCatching {
            navigator.credentials.create(
                options,
            ).await()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() ?: return null

        val attestationObjectBase64 = result.response.attestationObject.toBase64()
        val clientDataJSONBase64 = result.response.clientDataJSON.toBase64()

        return WebAuthModel.WebAuthCreateResult(
            attestationObjectBase64 = attestationObjectBase64,
            clientDataJSONBase64 = clientDataJSONBase64,
        )
    }

    public override suspend fun get(
        type: WebAuthModel.WebAuthModelType,
        challenge: String,
        domain: String,
    ): WebAuthModel.WebAuthGetResult? {
        val options = createOption(
            type = type,
            challenge = challenge,
            domain = domain,
            excludeCredentials = emptyList(),
        )

        val result = runCatching {
            navigator.credentials.get(
                options,
            ).await()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull() ?: return null
        console.log(result)
        return WebAuthModel.WebAuthGetResult(
            credentialId = result.id,
            base64ClientDataJSON = result.response.clientDataJSON.toBase64(),
            base64Signature = result.response.signature.toBase64(),
            base64UserHandle = result.response.userHandle.toBase64(),
            base64AuthenticatorData = result.response.authenticatorData.toBase64(),
        )
    }

    private fun createOption(
        type: WebAuthModel.WebAuthModelType,
        challenge: String,
        domain: String,
        excludeCredentials: List<CredentialsContainerCreatePublicKeyOptions.ExcludeCredential>,
    ): CredentialsContainerCreateOptions {
        return CredentialsContainerCreateOptions(
            publicKey = CredentialsContainerCreatePublicKeyOptions(
                challenge = Uint8Array(challenge.encodeToByteArray().toTypedArray()),
                pubKeyCredParams = arrayOf(
                    // ES256
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -7),
                    // RS256
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -257),
                    // Ed25519
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -8),
                ),
                excludeCredentials = excludeCredentials.toTypedArray(),
                authenticatorSelection = CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection(
                    authenticatorAttachment = when (type) {
                        WebAuthModel.WebAuthModelType.PLATFORM -> CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection.AUTH_TYPE_PLATFORM
                        WebAuthModel.WebAuthModelType.CROSS_PLATFORM -> CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection.AUTH_TYPE_CROSS_PLATFORM
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
    }

    private fun ArrayBuffer.toBase64(): String {
        return buildList {
            val uint8Array = Uint8Array(this@toBase64)
            for (index in 0 until this@toBase64.byteLength) {
                add(uint8Array[index])
            }
        }.toByteArray().encodeBase64()
    }
}
