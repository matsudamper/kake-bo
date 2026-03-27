package net.matsudamper.money.frontend.common.feature.uploader

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.work.WorkerFactory
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ServerHostConfig

public class ImageUploadDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, "image_upload_queue.db", null, 1) {

    private val dao: ImageUploadDao by lazy { ImageUploadDaoImpl(this) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE image_upload_queue (
                id TEXT PRIMARY KEY,
                moneyUsageId INTEGER NOT NULL,
                rawImageBytes BLOB NOT NULL,
                previewBytes BLOB,
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

    public companion object {
        public fun create(context: Context): ImageUploadDatabase {
            return ImageUploadDatabase(context)
        }
    }
}
