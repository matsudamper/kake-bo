package net.matsudamper.money.frontend.common.feature.webauth

import org.koin.core.scope.Scope

internal actual val factory: Factory = object : Factory() {
    override fun createWebAuthModule(scope: Scope): WebAuthModel {
        return WebAuthModelJsImpl()
    }
}
