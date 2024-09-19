package net.matsudamper.money.frontend.common.feature.webauth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import java.util.Base64
import kotlinx.serialization.json.Json
import net.matsudamper.money.frontend.common.base.Logger

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
                base64AuthenticatorData = Base64.getEncoder().encodeToString(
                    json.response.authenticatorData?.encodeToByteArray() ?: return null,
                ),
                base64ClientDataJSON = Base64.getEncoder().encodeToString(
                    json.response.clientDataJSON?.encodeToByteArray() ?: return null,
                ),
                base64Signature = Base64.getEncoder().encodeToString(
                    json.response.signature?.encodeToByteArray() ?: return null,
                ),
                base64UserHandle = Base64.getEncoder().encodeToString(
                    json.response.userHandle?.encodeToByteArray() ?: return null,
                ),
                credentialId = json.id ?: return null,
            )
        } else {
            null
        }
    }

    private fun configureGetCredentialRequest(
        challenge: String,
        domain: String,
    ): GetCredentialRequest {
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
            """
                {
                    "challenge": "$challenge",
                    "rpId": "$domain",
                    "userVerification": "required",
                    "timeout": 1800000
                }
            """.trimIndent().also { Logger.d("LOG", it) },
            null,
        )
        val getCredentialRequest = GetCredentialRequest(
            listOf(
                getPublicKeyCredentialOption,
            ),
        )
        return getCredentialRequest
    }

//    private fun signInWithSavedCredentials(getCredentialRequest: GetCredentialRequest): View.OnClickListener {
//        return View.OnClickListener {
//
//            lifecycleScope.launch {
//                configureViews(View.VISIBLE, false)
//
//                val data = getSavedCredentials(getCredentialRequest)
//
//                configureViews(View.INVISIBLE, true)
//
//                data?.let {
//                    showHome()
//                }
//            }
//        }
//    }
}
