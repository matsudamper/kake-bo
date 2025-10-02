package net.matsudamper.money.frontend.graphql

import net.matsudamper.money.frontend.common.feature.localstore.ServerConfigProvider
import org.koin.core.context.GlobalContext

private val serverConfigProvider: ServerConfigProvider by lazy {
    GlobalContext.get().get()
}

actual val serverProtocol: String get() = serverConfigProvider.getServerProtocol()
actual val serverHost: String get() = serverConfigProvider.getServerHost()
