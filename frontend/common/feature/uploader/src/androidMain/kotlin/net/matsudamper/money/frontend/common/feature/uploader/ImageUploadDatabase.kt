package net.matsudamper.money.frontend.common.feature.uploader

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ServerHostConfig

public class ImageUploadDatabase private constructor(private val db: ImageUploadRoomDatabase) {

    private val dao: ImageUploadRoomDao get() = db.dao()

    public fun createLocalStorage(context: Context): ImageUploadLocalStorage {
        return ImageUploadLocalStorageAndroidImpl(context = context)
    }

    public fun createQueue(context: Context, localStorage: ImageUploadLocalStorage): ImageUploadQueue {
        return ImageUploadQueueImpl(context = context, dao = dao, localStorage = localStorage)
    }

    public fun createWorkerFactory(
        dataStores: DataStores,
        graphqlClient: GraphqlClient,
        serverHostConfig: ServerHostConfig,
        localStorage: ImageUploadLocalStorage,
    ): WorkerFactory {
        return ImageUploadWorkerFactory(
            dao = dao,
            dataStores = dataStores,
            graphqlClient = graphqlClient,
            serverHostConfig = serverHostConfig,
            localStorage = localStorage,
        )
    }

    public fun recoverPendingUploads(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            // アプリ終了時にUPLOADING状態で止まっていたアイテムをPENDINGに戻す
            dao.resetUploadingToPending()
            val moneyUsageIds = dao.getDistinctMoneyUsageIdsWithPendingItems()
            moneyUsageIds.forEach { moneyUsageId ->
                val pendingItem = dao.getOldestPendingByMoneyUsageId(moneyUsageId) ?: return@forEach
                val request = OneTimeWorkRequestBuilder<ImageUploadWorker>()
                    .setInputData(workDataOf(ImageUploadWorker.KEY_RECORD_ID to pendingItem.id))
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    ImageUploadQueueImpl.uniqueWorkName(MoneyUsageId(moneyUsageId)),
                    ExistingWorkPolicy.KEEP,
                    request,
                )
            }
        }
    }

    public companion object {
        public fun create(context: Context): ImageUploadDatabase {
            val dbFile = context.getDatabasePath("image_upload_queue.db")
            val db = Room.databaseBuilder<ImageUploadRoomDatabase>(
                context = context,
                name = dbFile.absolutePath,
            )
                .setDriver(AndroidSQLiteDriver())
                .addMigrations(ImageUploadRoomDatabase.MIGRATION_1_2, ImageUploadRoomDatabase.MIGRATION_2_3, ImageUploadRoomDatabase.MIGRATION_3_4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            return ImageUploadDatabase(db)
        }
    }
}
