package net.matsudamper.money.frontend.common.di

import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModel
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModelJsImpl
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.GraphqlClientImpl
import org.koin.core.scope.Scope

internal actual val factory: Factory = object : Factory() {
    override fun createWebAuthModule(scope: Scope): WebAuthModel {
        return WebAuthModelJsImpl()
    }

    override fun createGraphQlClient(): GraphqlClient {
        return GraphqlClientImpl(
            interceptors = listOf(),
        )
    }
}
