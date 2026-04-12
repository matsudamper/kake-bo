package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_usage_records")
internal data class NotificationUsageEntity(
    @PrimaryKey
    @ColumnInfo(name = "notificationKey")
    val notificationKey: String,
    @ColumnInfo(name = "packageName")
    val packageName: String,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "postedAtEpochMillis")
    val postedAtEpochMillis: Long,
    @ColumnInfo(name = "receivedAtEpochMillis")
    val receivedAtEpochMillis: Long,
    @ColumnInfo(name = "isAdded")
    val isAdded: Boolean = false,
    @ColumnInfo(name = "moneyUsageId")
    val moneyUsageId: Int? = null,
)
