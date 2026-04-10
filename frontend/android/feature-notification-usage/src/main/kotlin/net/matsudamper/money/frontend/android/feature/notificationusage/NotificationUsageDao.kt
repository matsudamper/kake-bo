package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NotificationUsageDao {
    @Query(
        """
        SELECT *
        FROM notification_usage_records
        ORDER BY postedAtEpochMillis DESC, receivedAtEpochMillis DESC
        """,
    )
    fun observeAll(): Flow<List<NotificationUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotificationUsageEntity)

    @Query(
        """
        SELECT *
        FROM notification_usage_records
        WHERE notificationKey = :notificationKey
        LIMIT 1
        """,
    )
    suspend fun findByKey(notificationKey: String): NotificationUsageEntity?

    @Query(
        """
        UPDATE notification_usage_records
        SET isAdded = 1
        WHERE notificationKey = :notificationKey
        """,
    )
    suspend fun markAsAdded(notificationKey: String)
}
