package net.matsudamper.money

import android.app.Application
import kotlinx.coroutines.runBlocking
import net.matsudamper.money.frontend.common.di.AndroidModule
import net.matsudamper.money.frontend.common.di.DefaultModule
import net.matsudamper.money.frontend.common.feature.localstore.ServerConfigProvider
import org.koin.core.context.startKoin

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(
                AndroidModule.getModule(context = this@Application),
            )
            modules(DefaultModule.module)
        }

        val serverConfigProvider = ServerConfigProvider.create(this)
        if (serverConfigProvider.getServerHost().isEmpty()) {
            runBlocking {
                val protocol = BuildConfig.DEFAULT_SERVER_PROTOCOL
                val host = BuildConfig.DEFAULT_SERVER_HOST
                serverConfigProvider.setServerConfig(protocol, host)
            }
        }
    }
}
