package net.matsudamper.money.frontend.common.feature.webauth

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import java.util.Base64
import kotlinx.serialization.json.Json

public class WebAuthModelAndroidImpl(
    private val context: Context,
) : WebAuthModel {

    public override suspend fun create(
        id: String,
        name: String,
        type: WebAuthModel.WebAuthModelType,
        challenge: String,
        domain: String,
        base64ExcludeCredentialIdList: List<String>,
    ): WebAuthModel.WebAuthCreateResult? {
        val requestJson = buildCreateCredentialRequestJson(
            id = id,
            name = name,
            type = type,
            challenge = challenge,
            domain = domain,
            base64ExcludeCredentialIdList = base64ExcludeCredentialIdList,
        )
        val createRequest = CreatePublicKeyCredentialRequest(requestJson)
        val credentialManager = CredentialManager.create(context)

        val result = runCatching {
            credentialManager.createCredential(context, createRequest)
        }.getOrNull() ?: return null

        if (result !is CreatePublicKeyCredentialResponse) return null

        val json = Json.decodeFromString<AndroidWebAuthCreateResult>(result.registrationResponseJson)

        return WebAuthModel.WebAuthCreateResult(
            attestationObjectBase64 = json.response.attestationObject.base64UrlToBase64() ?: return null,
            clientDataJSONBase64 = json.response.clientDataJSON.base64UrlToBase64() ?: return null,
        )
    }

    public override suspend fun get(
        type: WebAuthModel.WebAuthModelType,
        challenge: String,
        domain: String,
    ): WebAuthModel.WebAuthGetResult? {
        val request = configureGetCredentialRequest(
            challenge = challenge,
            domain = domain,
        )
        val credentialManager = CredentialManager.create(context)
        val credentialResult = credentialManager.getCredential(context, request)

        return if (credentialResult.credential is PublicKeyCredential) {
            val cred = credentialResult.credential as PublicKeyCredential
            val json = Json.decodeFromString<AndroidWebAuthResult>(cred.authenticationResponseJson)
            WebAuthModel.WebAuthGetResult(
                base64AuthenticatorData = json.response.authenticatorData.base64UrlToBase64() ?: return null,
                base64ClientDataJSON = json.response.clientDataJSON.base64UrlToBase64() ?: return null,
                base64Signature = json.response.signature.base64UrlToBase64() ?: return null,
                base64UserHandle = json.response.userHandle.base64UrlToBase64() ?: return null,
                credentialId = json.id ?: return null,
            )
        } else {
            null
        }
    }

    private fun String?.base64UrlToBase64(): String? {
        this ?: return null
        return Base64.getEncoder()
            .encode(
                Base64.getUrlDecoder()
                    .decode(this),
            )
            .decodeToString()
    }

    private fun buildCreateCredentialRequestJson(
        id: String,
        name: String,
        type: WebAuthModel.WebAuthModelType,
        challenge: String,
        domain: String,
        base64ExcludeCredentialIdList: List<String>,
    ): String {
        val base64Challenge = Base64.getEncoder().encodeToString(challenge.toByteArray())
        val base64UserId = Base64.getEncoder().encodeToString(id.toByteArray())
        val authenticatorAttachment = when (type) {
            WebAuthModel.WebAuthModelType.PLATFORM -> "platform"
            WebAuthModel.WebAuthModelType.CROSS_PLATFORM -> "cross-platform"
        }
        val excludeCredentialsJson = base64ExcludeCredentialIdList.joinToString(",") { credentialId ->
            """{"type": "public-key", "id": "$credentialId"}"""
        }
        return """
            {
                "challenge": "$base64Challenge",
                "rp": {
                    "name": "$domain",
                    "id": "$domain"
                },
                "user": {
                    "id": "$base64UserId",
                    "name": "$name",
                    "displayName": "$name"
                },
                "pubKeyCredParams": [
                    {"type": "public-key", "alg": -7},
                    {"type": "public-key", "alg": -257},
                    {"type": "public-key", "alg": -8}
                ],
                "authenticatorSelection": {
                    "authenticatorAttachment": "$authenticatorAttachment",
                    "userVerification": "required",
                    "residentKey": "required"
                },
                "excludeCredentials": [$excludeCredentialsJson],
                "timeout": 1800000
            }
        """.trimIndent()
    }

    private fun configureGetCredentialRequest(
        challenge: String,
        domain: String,
    ): GetCredentialRequest {
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
            """
                {
                    "challenge": "${Base64.getEncoder().encodeToString(challenge.toByteArray())}",
                    "rpId": "$domain",
                    "userVerification": "required",
                    "timeout": 1800000
                }
            """.trimIndent(),
            null,
        )
        val getCredentialRequest = GetCredentialRequest(
            listOf(
                getPublicKeyCredentialOption,
            ),
        )
        return getCredentialRequest
    }
}
