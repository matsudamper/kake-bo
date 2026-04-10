package net.matsudamper.money.frontend.common.base

import kotlinx.coroutines.flow.Flow

public interface AppSettingsRepository {
    public val showImagesInMonthlyScreen: Flow<Boolean>
    public fun notificationUsageAutoAddEnabled(filterId: String): Flow<Boolean>

    public fun setShowImagesInMonthlyScreen(value: Boolean)
    public fun setNotificationUsageAutoAddEnabled(filterId: String, value: Boolean)
}
