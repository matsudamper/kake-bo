package net.matsudamper.money

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import net.matsudamper.money.frontend.android.feature.notificationusage.NotificationUsageModule
import net.matsudamper.money.frontend.common.di.AndroidModule
import net.matsudamper.money.frontend.common.di.DefaultModule
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.common.feature.uploader.ImageUploadDatabase
import org.koin.core.context.startKoin

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            modules(
                AndroidModule.getModule(context = this@Application),
            )
            modules(NotificationUsageModule.module)
            modules(DefaultModule.module)
        }

        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(koin.koin.get<WorkerFactory>())
                .build(),
        )

        koin.koin.get<ImageUploadDatabase>().recoverPendingUploads(this)

        initializeImageLoader(
            context = this@Application,
            sessionDataStore = koin.koin.get<DataStores>().sessionDataStore,
        )
    }
}
