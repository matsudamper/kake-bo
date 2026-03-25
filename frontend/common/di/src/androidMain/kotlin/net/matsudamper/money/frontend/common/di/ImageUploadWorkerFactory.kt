package net.matsudamper.money.frontend.common.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import net.matsudamper.money.frontend.common.feature.localstore.generated.Session

public class ImageUploadWorkerFactory(
    private val sessionDataStore: DataStore<Session>,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return if (workerClassName == ImageUploadWorker::class.java.name) {
            ImageUploadWorker(appContext, workerParameters, sessionDataStore)
        } else {
            null
        }
    }
}
