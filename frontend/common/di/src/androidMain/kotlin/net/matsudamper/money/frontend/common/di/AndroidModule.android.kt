package net.matsudamper.money.frontend.common.di

import android.app.Application
import android.content.Context
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.common.feature.localstore.ServerConfigProvider
import org.koin.dsl.module

object AndroidModule {
    fun getModule(context: Application) = module {
        factory<Context> { context }
        factory<Application> { context }
        factory<DataStores> { DataStores.create(context = get()) }
        single<ServerConfigProvider> { ServerConfigProvider.create(context = get()) }
    }
}
