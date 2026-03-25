package net.matsudamper.money

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import net.matsudamper.money.frontend.common.di.AndroidModule
import net.matsudamper.money.frontend.common.di.DefaultModule
import net.matsudamper.money.frontend.common.di.ImageUploadWorkerFactory
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

        val dataStores = koin.koin.get<DataStores>()

        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(ImageUploadWorkerFactory(dataStores.sessionDataStore))
                .build(),
        )

        initializeImageLoader(
            context = this@Application,
            sessionDataStore = dataStores.sessionDataStore,
        )
    }
}
