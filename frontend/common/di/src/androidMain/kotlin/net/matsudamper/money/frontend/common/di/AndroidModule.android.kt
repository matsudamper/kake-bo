package net.matsudamper.money.frontend.common.di

import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.graphql.ServerHostConfig
import net.matsudamper.money.frontend.graphql.serverHost
import net.matsudamper.money.frontend.graphql.serverProtocol
import org.koin.dsl.module

object AndroidModule {
    fun getModule(context: Application) = module {
        factory<Context> { context }
        factory<Application> { context }
        single<DataStores> { DataStores.create(context = get()) }
        single<ServerHostConfig> {
            val dataStores = get<DataStores>()
            val savedSession = runBlocking { dataStores.sessionDataStore.data.firstOrNull() }
            val savedHost = savedSession?.serverHost.orEmpty()
            val defaultHost = serverHost
            val protocol = serverProtocol.ifEmpty { "https" }
            ServerHostConfig(
                protocol = protocol,
                defaultHost = defaultHost,
                savedHost = savedHost,
            )
        }
    }
}
