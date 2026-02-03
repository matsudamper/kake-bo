package net.matsudamper.money.frontend.common.feature.webauth

import android.content.Context
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
        TODO()
    }

    public override suspend fun get(
        userId: String,
        name: String,
        type: WebAuthModel.WebAuthModelType,
        challenge: String,
        domain: String,
        allowCredentials: List<WebAuthModel.AllowCredential>,
    ): WebAuthModel.WebAuthGetResult? {
        val request = configureGetCredentialRequest(
            challenge = challenge,
            domain = domain,
            allowCredentials = allowCredentials,
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

    private fun configureGetCredentialRequest(
        challenge: String,
        domain: String,
        allowCredentials: List<WebAuthModel.AllowCredential>,
    ): GetCredentialRequest {
        val allowCredentialsJson = if (allowCredentials.isNotEmpty()) {
            allowCredentials.joinToString(separator = ",", prefix = "[", postfix = "]") { credential ->
                """{"id":"${credential.id}","type":"${credential.type}"}"""
            }
        } else {
            "[]"
        }
        
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
            """
                {
                    "challenge": "${Base64.getEncoder().encodeToString(challenge.toByteArray())}",
                    "rpId": "$domain",
                    "allowCredentials": $allowCredentialsJson,
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
