package net.matsudamper.money.frontend.common.feature.webauth

import kotlin.js.Promise
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

internal external interface CredentialsContainer : JsAny {
    fun get(options: CredentialsContainerCreateOptions): Promise<PublicKeyCredential>

    fun create(options: CredentialsContainerCreateOptions): Promise<CredentialsContainerCreateResult>
}

internal external interface PublicKeyCredential : JsAny {
    val id: String
    val response: AuthenticatorAssertionResponse
}

internal external interface AuthenticatorAssertionResponse : JsAny {
    val authenticatorData: ArrayBuffer
    val clientDataJSON: ArrayBuffer
    val signature: ArrayBuffer
    val userHandle: ArrayBuffer
}

internal external interface CredentialsContainerCreateResult : JsAny {
    val response: AuthenticatorAttestationResponse
}

internal external interface AuthenticatorAttestationResponse : JsAny {
    val attestationObject: ArrayBuffer
    val clientDataJSON: ArrayBuffer
}

internal external interface CredentialsContainerCreateOptions : JsAny

internal external interface PublicKeyCredentialUser : JsAny

internal external interface PublicKeyCredentialRp : JsAny

internal external interface PubKeyCredParams : JsAny

internal external interface ExcludeCredential : JsAny

internal external interface AuthenticatorSelection : JsAny

internal object AuthenticatorAttachmentType {
    const val PLATFORM = "platform"
    const val CROSS_PLATFORM = "cross-platform"
}

internal fun credentialsContainer(): CredentialsContainer = js("navigator.credentials")

internal fun CredentialsContainerCreateOptions(
    challenge: Int8Array,
    user: PublicKeyCredentialUser?,
    rp: PublicKeyCredentialRp,
    pubKeyCredParams: JsArray<PubKeyCredParams>,
    excludeCredentials: JsArray<ExcludeCredential>,
    authenticatorSelection: AuthenticatorSelection,
): CredentialsContainerCreateOptions = js(
    """({
        publicKey: {
            challenge: challenge,
            user: user,
            rp: rp,
            pubKeyCredParams: pubKeyCredParams,
            excludeCredentials: excludeCredentials,
            authenticatorSelection: authenticatorSelection,
        },
    })""",
)

internal fun PublicKeyCredentialUser(
    id: Int8Array,
    name: String,
    displayName: String,
): PublicKeyCredentialUser = js("({ id: id, name: name, displayName: displayName })")

internal fun PublicKeyCredentialRp(
    name: String,
    id: String,
): PublicKeyCredentialRp = js("({ name: name, id: id })")

internal fun PubKeyCredParams(
    type: String,
    alg: Int,
): PubKeyCredParams = js("({ type: type, alg: alg })")

internal fun ExcludeCredential(
    id: Int8Array,
    type: String,
): ExcludeCredential = js("({ id: id, type: type })")

internal fun AuthenticatorSelection(
    authenticatorAttachment: String?,
    requireResidentKey: Boolean,
    userVerification: String,
    residentKey: String,
): AuthenticatorSelection = js(
    """({
        authenticatorAttachment: authenticatorAttachment,
        requireResidentKey: requireResidentKey,
        userVerification: userVerification,
        residentKey: residentKey,
    })""",
)
