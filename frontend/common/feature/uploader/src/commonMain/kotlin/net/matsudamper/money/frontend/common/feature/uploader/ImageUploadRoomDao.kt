package net.matsudamper.money.frontend.common.feature.uploader

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ImageUploadRoomDao {
    @Query("SELECT * FROM image_upload_queue WHERE moneyUsageId = :moneyUsageId ORDER BY createdAt ASC")
    fun observeByMoneyUsageId(moneyUsageId: Int): Flow<List<ImageUploadRoomEntity>>

    @Insert
    suspend fun insert(entity: ImageUploadRoomEntity)

    @Query("SELECT * FROM image_upload_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ImageUploadRoomEntity?

    @Query("UPDATE image_upload_queue SET workManagerId = :workManagerId WHERE id = :id")
    suspend fun updateWorkManagerId(id: String, workManagerId: String)

    @Query("UPDATE image_upload_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE image_upload_queue SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatusWithError(id: String, status: String, errorMessage: String?)

    @Query("DELETE FROM image_upload_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM image_upload_queue WHERE moneyUsageId = :moneyUsageId AND status = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getOldestPendingByMoneyUsageId(moneyUsageId: Int): ImageUploadRoomEntity?

    @Query("SELECT DISTINCT moneyUsageId FROM image_upload_queue WHERE status = 'PENDING'")
    suspend fun getDistinctMoneyUsageIdsWithPendingItems(): List<Int>

    @Query("UPDATE image_upload_queue SET status = 'PENDING', workManagerId = NULL, errorMessage = NULL WHERE status = 'UPLOADING'")
    suspend fun resetUploadingToPending()

    @Query("SELECT * FROM image_upload_queue ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ImageUploadRoomEntity>>
}
