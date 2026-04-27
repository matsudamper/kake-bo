package net.matsudamper.money.frontend.common.feature.webauth

import kotlinx.coroutines.await
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.feature.webauth.CredentialsContainerCreatePublicKeyOptions.User
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

private const val TAG = "WebAuthModelJsImpl"

public class WebAuthModelJsImpl : WebAuthModel {
    public override suspend fun create(
        id: String,
        name: String,
        challenge: String,
        domain: String,
        base64ExcludeCredentialIdList: List<String>,
    ): WebAuthModel.WebAuthCreateResult? {
        val options = CredentialsContainerCreateOptions(
            publicKey = CredentialsContainerCreatePublicKeyOptions(
                challenge = Uint8Array(challenge.encodeToByteArray().toTypedArray()),
                user = User(
                    id = Uint8Array(id.encodeToByteArray().toTypedArray()),
                    name = name,
                    displayName = name,
                ),
                pubKeyCredParams = arrayOf(
                    // ES256
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -7),
                    // RS256
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -257),
                    // Ed25519
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -8),
                ),
                excludeCredentials = base64ExcludeCredentialIdList.map {
                    CredentialsContainerCreatePublicKeyOptions.ExcludeCredential(
                        id = it.decodeBase64Bytes(),
                        type = "public-key",
                    )
                }.toTypedArray(),
                authenticatorSelection = CredentialsContainerCreatePublicKeyOptions.AuthenticatorSelection(
                    authenticatorAttachment = null,
                    userVerification = "required",
                    residentKey = "required",
                ),
                rp = CredentialsContainerCreatePublicKeyOptions.Rp(
                    name = domain,
                    id = domain,
                ),
            ),
        )
        val result = runCatching {
            navigator.credentials.create(
                options,
            ).await()
        }.onFailure {
            Logger.e(TAG, it)
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
        val options = CredentialsContainerCreateOptions(
            publicKey = CredentialsContainerCreatePublicKeyOptions(
                challenge = Uint8Array(challenge.encodeToByteArray().toTypedArray()),
                user = null,
                pubKeyCredParams = arrayOf(
                    // ES256
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -7),
                    // RS256
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -257),
                    // Ed25519
                    CredentialsContainerCreatePublicKeyOptions.PubKeyCredParams("public-key", -8),
                ),
                excludeCredentials = arrayOf(),
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

        val result = runCatching {
            navigator.credentials.get(
                options,
            ).await()
        }.onFailure {
            Logger.e(TAG, it)
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

    private fun ArrayBuffer.toBase64(): String {
        return buildList {
            val uint8Array = Uint8Array(this@toBase64)
            for (index in 0 until this@toBase64.byteLength) {
                add(uint8Array[index])
            }
        }.toByteArray().encodeBase64()
    }
}
