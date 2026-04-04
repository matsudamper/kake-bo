package net.matsudamper.money.frontend.common.feature.uploader

import kotlinx.coroutines.flow.Flow

internal interface ImageUploadDao {
    fun observeByMoneyUsageId(moneyUsageId: Int): Flow<List<ImageUploadEntity>>

    suspend fun insert(entity: ImageUploadEntity)

    suspend fun getById(id: String): ImageUploadEntity?

    suspend fun updateWorkManagerId(id: String, workManagerId: String)

    suspend fun updateStatus(id: String, status: String)

    suspend fun updateStatusWithError(id: String, status: String, errorMessage: String?)

    suspend fun deleteById(id: String)

    suspend fun getOldestPendingByMoneyUsageId(moneyUsageId: Int): ImageUploadEntity?

    suspend fun getDistinctMoneyUsageIdsWithPendingItems(): List<Int>

    suspend fun resetUploadingToPending()
}
