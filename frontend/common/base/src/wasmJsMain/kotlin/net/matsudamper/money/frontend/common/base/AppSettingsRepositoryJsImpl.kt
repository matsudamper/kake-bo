package net.matsudamper.money.frontend.common.base

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

public class AppSettingsRepositoryJsImpl : AppSettingsRepository {
    private val showImagesKey = "showImagesInMonthlyScreen"
    private val notificationUsageAutoAddEnabledFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()

    private val _showImagesInMonthlyScreen = MutableStateFlow(
        localStorage.getItem(showImagesKey)?.toBoolean() ?: false,
    )

    override val showImagesInMonthlyScreen: Flow<Boolean> = _showImagesInMonthlyScreen

    override fun setShowImagesInMonthlyScreen(value: Boolean) {
        localStorage.setItem(showImagesKey, value.toString())
        _showImagesInMonthlyScreen.value = value
    }

    override fun notificationUsageAutoAddEnabled(filterId: String): Flow<Boolean> {
        return notificationUsageAutoAddEnabledFlows.getOrPut(filterId) {
            MutableStateFlow(
                localStorage.getItem(notificationUsageAutoAddEnabledKey(filterId))?.toBoolean() ?: false,
            )
        }
    }

    override fun setNotificationUsageAutoAddEnabled(filterId: String, value: Boolean) {
        localStorage.setItem(notificationUsageAutoAddEnabledKey(filterId), value.toString())
        notificationUsageAutoAddEnabledFlows.getOrPut(filterId) {
            MutableStateFlow(value)
        }.value = value
    }

    private fun notificationUsageAutoAddEnabledKey(filterId: String): String {
        return "notificationUsageAutoAddEnabled.$filterId"
    }
}
