package net.matsudamper.money.frontend.common.feature.webauth

import android.content.Context

public class WebAuthModelAndroidImpl(
    private val context: Context,
): WebAuthModel {
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
        TODO()
    }

//    private fun configureGetCredentialRequest(): GetCredentialRequest {
//        val getPublicKeyCredentialOption =
//            GetPublicKeyCredentialOption(fetchAuthJsonFromServer(), null)
//        val getPasswordOption = GetPasswordOption()
//        val getCredentialRequest = GetCredentialRequest(
//            listOf(
//                getPublicKeyCredentialOption,
//                getPasswordOption
//            )
//        )
//        return getCredentialRequest
//    }
//
//
//    private fun configureAutofill(getCredentialRequest: GetCredentialRequest) {
//        binding.textUsername
//            .pendingGetCredentialRequest = PendingGetCredentialRequest(
//            getCredentialRequest
//        ) { response ->
//            if (response.credential is PublicKeyCredential) {
//                DataProvider.setSignedInThroughPasskeys(true)
//            }
//            if (response.credential is PasswordCredential) {
//                DataProvider.setSignedInThroughPasskeys(false)
//            }
//            showHome()
//        }
//    }
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
