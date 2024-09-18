package net.matsudamper.money.frontend.common.base.navigator

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public actual object WebAuthModel {
    actual suspend fun create(
        id: String,
        name: String,
        type: WebAuthModelType,
        challenge: String,
        domain: String,
        base64ExcludeCredentialIdList: List<String>
    ): WebAuthCreateResult? {
        TODO("Not yet implemented")
    }

    actual suspend fun get(
        userId: String,
        name: String,
        type: WebAuthModelType,
        challenge: String,
        domain: String
    ): WebAuthGetResult? {
        TODO("Not yet implemented")
    }

}