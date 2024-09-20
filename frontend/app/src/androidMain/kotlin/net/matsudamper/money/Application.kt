package net.matsudamper.money

import android.app.Application
import android.content.Context
import net.matsudamper.money.frontend.common.di.DefaultModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(
                module {
                    single<Application> { this@Application }
                    single<Context> { this@Application }
                },
            )
            modules(DefaultModule.module)
        }
    }
}
