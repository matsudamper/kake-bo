package net.matsudamper.money.frontend.common.di

import net.matsudamper.money.frontend.common.base.ImageUploadClient
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModel
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModelJsImpl
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.GraphqlClientImpl
import net.matsudamper.money.frontend.graphql.serverHost
import net.matsudamper.money.frontend.graphql.serverProtocol
import org.koin.core.scope.Scope

internal actual val factory: Factory = object : Factory() {
    override fun createWebAuthModule(scope: Scope): WebAuthModel {
        return WebAuthModelJsImpl()
    }

    override fun createPhotoUploadClient(scope: Scope): ImageUploadClient {
        return ImageUploadClientJsImpl()
    }

    override fun createGraphQlClient(scope: Scope): GraphqlClient {
        return GraphqlClientImpl(
            interceptors = listOf(),
            serverUrl = "$serverProtocol://$serverHost/query",
            onServerUrlChanged = {},
        )
    }
}
