package net.matsudamper.money.frontend.common.feature.webauth

import org.koin.core.scope.Scope
import org.koin.dsl.module

internal expect val factory: Factory

object DefaultModule {
    val module = module {
        factory<WebAuthModel> { factory.createWebAuthModule(scope = this) }
    }
}

internal abstract class Factory {
    abstract fun createWebAuthModule(scope: Scope): WebAuthModel
}
