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
        ORDER BY receivedAtEpochMillis DESC, postedAtEpochMillis DESC
        """,
    )
    fun observeAll(): Flow<List<NotificationUsageEntity>>

    @Query(
        """
        SELECT *
        FROM notification_usage_records
        WHERE isAdded = 0
        ORDER BY receivedAtEpochMillis DESC, postedAtEpochMillis DESC
        """,
    )
    fun observeNotAdded(): Flow<List<NotificationUsageEntity>>

    @Query(
        """
        SELECT *
        FROM notification_usage_records
        WHERE isAdded = 1
        ORDER BY receivedAtEpochMillis DESC, postedAtEpochMillis DESC
        """,
    )
    fun observeAdded(): Flow<List<NotificationUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationUsageEntity)

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
        SELECT *
        FROM notification_usage_records
        WHERE notificationKey = :notificationKey
        LIMIT 1
        """,
    )
    fun observeByKey(notificationKey: String): Flow<NotificationUsageEntity?>

    @Query(
        """
        UPDATE notification_usage_records
        SET isAdded = 1,
            moneyUsageId = :moneyUsageId
        WHERE notificationKey = :notificationKey
        """,
    )
    suspend fun markAsAdded(notificationKey: String, moneyUsageId: Int?)

    @Query(
        """
        DELETE FROM notification_usage_records
        WHERE notificationKey = :notificationKey
        """,
    )
    suspend fun deleteByKey(notificationKey: String)
}
