package net.matsudamper.money.frontend.common.di

import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import org.koin.core.scope.Scope
import org.koin.dsl.module

internal expect val factory: Factory

object DefaultModule {
    val module = module {
        factory<WebAuthModel> { factory.createWebAuthModule(scope = this) }
        single<GraphqlClient> { factory.createGraphQlClient() }
    }
}

internal abstract class Factory {
    abstract fun createWebAuthModule(scope: Scope): WebAuthModel
    abstract fun createGraphQlClient(): GraphqlClient
}
