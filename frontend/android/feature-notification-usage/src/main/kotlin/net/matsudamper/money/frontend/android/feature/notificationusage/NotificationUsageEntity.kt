package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_usage_records")
internal data class NotificationUsageEntity(
    @PrimaryKey
    val notificationKey: String,
    val packageName: String,
    val text: String,
    val postedAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
    val isAdded: Boolean = false,
)
