package net.matsudamper.money.frontend.common.base

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

public class AppSettingsRepositoryAndroidImpl(context: Context) : AppSettingsRepository {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val showImagesKey = "showImagesInMonthlyScreen"
    private val notificationUsageAutoAddEnabledFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()

    private val _showImagesInMonthlyScreen = MutableStateFlow(
        prefs.getBoolean(showImagesKey, false),
    )

    override val showImagesInMonthlyScreen: Flow<Boolean> = _showImagesInMonthlyScreen

    override fun setShowImagesInMonthlyScreen(value: Boolean) {
        prefs.edit { putBoolean(showImagesKey, value) }
        _showImagesInMonthlyScreen.value = value
    }

    override fun notificationUsageAutoAddEnabled(filterId: String): Flow<Boolean> {
        return synchronized(notificationUsageAutoAddEnabledFlows) {
            notificationUsageAutoAddEnabledFlows.getOrPut(filterId) {
                MutableStateFlow(
                    prefs.getBoolean(notificationUsageAutoAddEnabledKey(filterId), false),
                )
            }
        }
    }

    override fun setNotificationUsageAutoAddEnabled(filterId: String, value: Boolean) {
        prefs.edit { putBoolean(notificationUsageAutoAddEnabledKey(filterId), value) }
        synchronized(notificationUsageAutoAddEnabledFlows) {
            notificationUsageAutoAddEnabledFlows.getOrPut(filterId) {
                MutableStateFlow(value)
            }
        }.value = value
    }

    private fun notificationUsageAutoAddEnabledKey(filterId: String): String {
        return "notificationUsageAutoAddEnabled.$filterId"
    }
}
