package net.matsudamper.money.frontend.common.feature.webauth

import kotlin.js.toJsArray
import kotlinx.coroutines.await
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import net.matsudamper.money.frontend.common.base.Logger
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array

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
            challenge = challenge.encodeToByteArray().toInt8Array(),
            user = PublicKeyCredentialUser(
                id = id.encodeToByteArray().toInt8Array(),
                name = name,
                displayName = name,
            ),
            pubKeyCredParams = supportedPubKeyCredParams(),
            excludeCredentials = base64ExcludeCredentialIdList.map {
                ExcludeCredential(
                    id = it.decodeBase64Bytes().toInt8Array(),
                    type = "public-key",
                )
            }.toJsArray(),
            authenticatorSelection = AuthenticatorSelection(
                authenticatorAttachment = null,
                requireResidentKey = true,
                userVerification = "required",
                residentKey = "required",
            ),
            rp = PublicKeyCredentialRp(
                name = domain,
                id = domain,
            ),
        )
        val result = runCatching {
            credentialsContainer().create(
                options,
            ).await<CredentialsContainerCreateResult>()
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
            challenge = challenge.encodeToByteArray().toInt8Array(),
            user = null,
            pubKeyCredParams = supportedPubKeyCredParams(),
            excludeCredentials = listOf<ExcludeCredential>().toJsArray(),
            authenticatorSelection = AuthenticatorSelection(
                authenticatorAttachment = when (type) {
                    WebAuthModel.WebAuthModelType.PLATFORM -> AuthenticatorAttachmentType.PLATFORM
                    WebAuthModel.WebAuthModelType.CROSS_PLATFORM -> AuthenticatorAttachmentType.CROSS_PLATFORM
                },
                requireResidentKey = true,
                userVerification = "required",
                residentKey = "required",
            ),
            rp = PublicKeyCredentialRp(
                name = domain,
                id = domain,
            ),
        )

        val result = runCatching {
            credentialsContainer().get(
                options,
            ).await<PublicKeyCredential>()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull() ?: return null
        return WebAuthModel.WebAuthGetResult(
            credentialId = result.id,
            base64ClientDataJSON = result.response.clientDataJSON.toBase64(),
            base64Signature = result.response.signature.toBase64(),
            base64UserHandle = result.response.userHandle.toBase64(),
            base64AuthenticatorData = result.response.authenticatorData.toBase64(),
        )
    }

    private fun supportedPubKeyCredParams(): JsArray<PubKeyCredParams> {
        return listOf(
            // ES256
            PubKeyCredParams("public-key", -7),
            // RS256
            PubKeyCredParams("public-key", -257),
            // Ed25519
            PubKeyCredParams("public-key", -8),
        ).toJsArray()
    }

    private fun ArrayBuffer.toBase64(): String {
        return Int8Array(this).toByteArray().encodeBase64()
    }
}
