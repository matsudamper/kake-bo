package net.matsudamper.money

import android.app.Application
import net.matsudamper.money.frontend.common.di.AndroidModule
import net.matsudamper.money.frontend.common.di.DefaultModule
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
    }
}
