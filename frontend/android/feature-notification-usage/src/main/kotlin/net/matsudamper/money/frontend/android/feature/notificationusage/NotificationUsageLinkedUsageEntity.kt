package net.matsudamper.money.frontend.android.feature.notificationusage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_usage_linked_usages",
    foreignKeys = [
        ForeignKey(
            entity = NotificationUsageEntity::class,
            parentColumns = ["notificationKey"],
            childColumns = ["notificationKey"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("notificationKey")],
)
internal data class NotificationUsageLinkedUsageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "notificationKey")
    val notificationKey: String,
    @ColumnInfo(name = "moneyUsageId")
    val moneyUsageId: Int,
)
