package net.matsudamper.money.frontend.common.feature.uploader

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ServerHostConfig

internal class ImageUploadWorkerFactory(
    private val dao: ImageUploadDao,
    private val dataStores: DataStores,
    private val graphqlClient: GraphqlClient,
    private val serverHostConfig: ServerHostConfig,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        if (workerClassName != ImageUploadWorker::class.java.name) return null
        return ImageUploadWorker(
            appContext = appContext,
            params = workerParameters,
            dao = dao,
            dataStores = dataStores,
            graphqlClient = graphqlClient,
            serverHostConfig = serverHostConfig,
        )
    }
}
