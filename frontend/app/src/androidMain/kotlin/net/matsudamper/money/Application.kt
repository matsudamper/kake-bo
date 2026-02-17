package net.matsudamper.money

import android.app.Application
import net.matsudamper.money.frontend.common.di.AndroidModule
import net.matsudamper.money.frontend.common.di.DefaultModule
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import org.koin.core.context.startKoin

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            modules(
                AndroidModule.getModule(context = this@Application),
            )
            modules(DefaultModule.module)
        }

        initializeImageLoader(
            context = this@Application,
            sessionDataStore = koin.koin.get<DataStores>().sessionDataStore,
        )
    }
}
