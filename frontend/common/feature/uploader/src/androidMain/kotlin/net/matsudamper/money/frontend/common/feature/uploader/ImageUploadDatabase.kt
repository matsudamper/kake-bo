package net.matsudamper.money.frontend.common.feature.uploader

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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

public class ImageUploadDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, "image_upload_queue.db", null, 2) {

    private val dao: ImageUploadDao by lazy { ImageUploadDaoImpl(this) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE image_upload_queue (
                id TEXT PRIMARY KEY,
                moneyUsageId INTEGER NOT NULL,
                status TEXT NOT NULL,
                workManagerId TEXT,
                errorMessage TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS image_upload_queue")
        onCreate(db)
    }

    public fun createQueue(context: Context): ImageUploadQueue {
        return ImageUploadQueueImpl(context = context, dao = dao)
    }

    public fun createWorkerFactory(
        dataStores: DataStores,
        graphqlClient: GraphqlClient,
        serverHostConfig: ServerHostConfig,
    ): WorkerFactory {
        return ImageUploadWorkerFactory(
            dao = dao,
            dataStores = dataStores,
            graphqlClient = graphqlClient,
            serverHostConfig = serverHostConfig,
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
            return ImageUploadDatabase(context)
        }
    }
}
