package net.matsudamper.money.frontend.common.feature.localstore

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

public interface ServerConfigProvider {
    public fun getServerProtocol(): String
    public fun getServerHost(): String
    public suspend fun setServerConfig(protocol: String, host: String)

    public companion object {
        public fun create(context: Context): ServerConfigProvider = ServerConfigProviderImpl(context)
    }
}

internal class ServerConfigProviderImpl(
    private val context: Context,
) : ServerConfigProvider {
    private val dataStores = DataStores.create(context)

    override fun getServerProtocol(): String {
        return runBlocking {
            dataStores.sessionDataStore.data.first().serverProtocol.orEmpty().ifEmpty { "https" }
        }
    }

    override fun getServerHost(): String {
        return runBlocking {
            dataStores.sessionDataStore.data.first().serverHost.orEmpty()
        }
    }

    override suspend fun setServerConfig(protocol: String, host: String) {
        dataStores.sessionDataStore.updateData {
            it.toBuilder()
                .setServerProtocol(protocol)
                .setServerHost(host)
                .build()
        }
    }
}
