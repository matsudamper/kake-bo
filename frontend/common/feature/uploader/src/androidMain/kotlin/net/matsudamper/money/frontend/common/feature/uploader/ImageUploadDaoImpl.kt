package net.matsudamper.money.frontend.common.feature.uploader

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class ImageUploadDaoImpl(private val db: SQLiteOpenHelper) : ImageUploadDao {

    private val changeNotifier = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        changeNotifier.tryEmit(Unit)
    }

    override fun observeByMoneyUsageId(moneyUsageId: Int): Flow<List<ImageUploadEntity>> {
        return changeNotifier.map {
            withContext(Dispatchers.IO) {
                queryByMoneyUsageId(moneyUsageId)
            }
        }
    }

    private fun queryByMoneyUsageId(moneyUsageId: Int): List<ImageUploadEntity> {
        val cursor = db.readableDatabase.query(
            "image_upload_queue",
            arrayOf("id", "moneyUsageId", "status", "workManagerId", "errorMessage", "createdAt"),
            "moneyUsageId = ?",
            arrayOf(moneyUsageId.toString()),
            null,
            null,
            "createdAt ASC",
        )
        return cursor.use { cursorToEntities(it) }
    }

    override suspend fun insert(entity: ImageUploadEntity) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("id", entity.id)
                put("moneyUsageId", entity.moneyUsageId)
                put("status", entity.status)
                put("workManagerId", entity.workManagerId)
                put("errorMessage", entity.errorMessage)
                put("createdAt", entity.createdAt)
            }
            db.writableDatabase.insert("image_upload_queue", null, values)
        }
        changeNotifier.tryEmit(Unit)
    }

    override suspend fun getById(id: String): ImageUploadEntity? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                "image_upload_queue",
                arrayOf("id", "moneyUsageId", "status", "workManagerId", "errorMessage", "createdAt"),
                "id = ?",
                arrayOf(id),
                null,
                null,
                null,
            )
            cursor.use { cursorToEntities(it).firstOrNull() }
        }
    }

    override suspend fun updateWorkManagerId(id: String, workManagerId: String) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("workManagerId", workManagerId)
            }
            db.writableDatabase.update("image_upload_queue", values, "id = ?", arrayOf(id))
        }
        changeNotifier.tryEmit(Unit)
    }

    override suspend fun updateStatus(id: String, status: String) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("status", status)
            }
            db.writableDatabase.update("image_upload_queue", values, "id = ?", arrayOf(id))
        }
        changeNotifier.tryEmit(Unit)
    }

    override suspend fun updateStatusWithError(id: String, status: String, errorMessage: String?) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("status", status)
                put("errorMessage", errorMessage)
            }
            db.writableDatabase.update("image_upload_queue", values, "id = ?", arrayOf(id))
        }
        changeNotifier.tryEmit(Unit)
    }

    override suspend fun deleteById(id: String) {
        withContext(Dispatchers.IO) {
            db.writableDatabase.delete("image_upload_queue", "id = ?", arrayOf(id))
        }
        changeNotifier.tryEmit(Unit)
    }

    override suspend fun getOldestPendingByMoneyUsageId(moneyUsageId: Int): ImageUploadEntity? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                "image_upload_queue",
                arrayOf("id", "moneyUsageId", "status", "workManagerId", "errorMessage", "createdAt"),
                "moneyUsageId = ? AND status = ?",
                arrayOf(moneyUsageId.toString(), ImageUploadQueueImpl.STATUS_PENDING),
                null,
                null,
                "createdAt ASC",
                "1",
            )
            cursor.use { cursorToEntities(it).firstOrNull() }
        }
    }

    override suspend fun getDistinctMoneyUsageIdsWithPendingItems(): List<Int> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.rawQuery(
                "SELECT DISTINCT moneyUsageId FROM image_upload_queue WHERE status = ?",
                arrayOf(ImageUploadQueueImpl.STATUS_PENDING),
            )
            val ids = mutableListOf<Int>()
            cursor.use {
                while (it.moveToNext()) {
                    ids.add(it.getInt(0))
                }
            }
            ids
        }
    }

    override suspend fun resetUploadingToPending() {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("status", ImageUploadQueueImpl.STATUS_PENDING)
                putNull("workManagerId")
                putNull("errorMessage")
            }
            db.writableDatabase.update(
                "image_upload_queue",
                values,
                "status = ?",
                arrayOf(ImageUploadQueueImpl.STATUS_UPLOADING),
            )
        }
        changeNotifier.tryEmit(Unit)
    }

    private fun cursorToEntities(cursor: Cursor): List<ImageUploadEntity> {
        val list = mutableListOf<ImageUploadEntity>()
        while (cursor.moveToNext()) {
            list.add(
                ImageUploadEntity(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    moneyUsageId = cursor.getInt(cursor.getColumnIndexOrThrow("moneyUsageId")),
                    status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                    workManagerId = cursor.getString(cursor.getColumnIndexOrThrow("workManagerId")),
                    errorMessage = cursor.getString(cursor.getColumnIndexOrThrow("errorMessage")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                ),
            )
        }
        return list
    }
}
