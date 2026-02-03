package net.matsudamper.money.frontend.common.feature.webauth

public interface WebAuthModel {
    public suspend fun create(
        id: String,
        name: String,
        type: WebAuthModelType,
        challenge: String,
        domain: String,
        base64ExcludeCredentialIdList: List<String>,
    ): WebAuthCreateResult?

    public suspend fun get(
        userId: String,
        name: String,
        type: WebAuthModelType,
        challenge: String,
        domain: String,
        allowCredentials: List<AllowCredential>,
    ): WebAuthGetResult?

    public data class WebAuthCreateResult(
        val attestationObjectBase64: String,
        val clientDataJSONBase64: String,
    )

    public data class WebAuthGetResult(
        val base64AuthenticatorData: String,
        val base64ClientDataJSON: String,
        val base64Signature: String,
        val base64UserHandle: String,
        val credentialId: String,
    )

    public data class AllowCredential(
        val id: String,
        val type: String,
    )

    public enum class WebAuthModelType {
        PLATFORM,
        CROSS_PLATFORM,
    }
}
