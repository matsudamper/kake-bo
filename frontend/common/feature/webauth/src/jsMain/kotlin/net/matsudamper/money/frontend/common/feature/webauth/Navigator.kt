package net.matsudamper.money.frontend.common.feature.webauth

import kotlin.js.Promise
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array

internal external val navigator: Navigator

internal external interface Navigator {
    val credentials: CredentialsContainer
}

internal external interface CredentialsContainer {
    fun get(options: CredentialsContainerCreateOptions): Promise<PublicKeyCredential>

    fun create(options: CredentialsContainerCreateOptions): Promise<CredentialsContainerCreateResult>
}

@Suppress("OPT_IN_USAGE")
@JsExport
public class PublicKeyCredential(
    public val rawId: ArrayBuffer,
    public val response: AuthenticatorAssertionResponse,
    public val authenticatorAttachment: dynamic,
    public val id: String,
    public val type: String,
) {
    public data class AuthenticatorAssertionResponse(
        val authenticatorData: ArrayBuffer,
        val clientDataJSON: ArrayBuffer,
        val signature: ArrayBuffer,
        val userHandle: ArrayBuffer,
    )
}

@Suppress("OPT_IN_USAGE")
@JsExport
public class CredentialsContainerCreateOptions(
    public val publicKey: CredentialsContainerCreatePublicKeyOptions,
)

@Suppress("OPT_IN_USAGE")
@JsExport
public data class CredentialsContainerCreateResult(
    public val authenticatorAttachment: String,
    public val id: String,
    public val rawId: ArrayBuffer,
    public val response: Response,
    public val type: String,
) {
    public data class Response(
        public val attestationObject: ArrayBuffer,
        public val clientDataJSON: ArrayBuffer,
        public val transports: Array<String> = arrayOf(),
    )
}

@Suppress("OPT_IN_USAGE")
@JsExport
public class CredentialsContainerCreatePublicKeyOptions(
    public val challenge: Uint8Array,
    public val user: User,
    public val rp: Rp,
    public val pubKeyCredParams: Array<PubKeyCredParams>,
    public val excludeCredentials: Array<ExcludeCredential> = arrayOf(),
    public val authenticatorSelection: AuthenticatorSelection,
) {
    public class ExcludeCredential(
        public val id: ByteArray,
        public val type: String,
    )

    public data class Rp(
        val name: String,
        val id: String,
    )

    public data class User(
        val id: Uint8Array,
        val name: String,
        val displayName: String,
    )

    public data class PubKeyCredParams(
        val type: String,
        val alg: Int,
    )

    public data class AuthenticatorSelection(
        val authenticatorAttachment: String,
        val requireResidentKey: Boolean = true,
        val userVerification: String,
        val residentKey: String,
    ) {
        public companion object {
            internal const val AUTH_TYPE_PLATFORM = "platform"
            internal const val AUTH_TYPE_CROSS_PLATFORM = "cross-platform"
        }
    }
}
