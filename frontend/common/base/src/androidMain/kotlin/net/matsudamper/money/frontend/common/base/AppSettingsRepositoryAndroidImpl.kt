package net.matsudamper.money.frontend.common.base

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

public class AppSettingsRepositoryAndroidImpl(context: Context) : AppSettingsRepository {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val notificationUsageAutoAddEnabledFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    override fun notificationUsageAutoAddEnabled(filterId: String): Flow<Boolean> {
        if (!notificationUsageAutoAddEnabledFlow.value.containsKey(filterId)) {
            notificationUsageAutoAddEnabledFlow.update { current ->
                if (current.containsKey(filterId)) {
                    current
                } else {
                    current + (filterId to prefs.getBoolean(notificationUsageAutoAddEnabledKey(filterId), false))
                }
            }
        }
        return notificationUsageAutoAddEnabledFlow.map { it[filterId] ?: false }
    }

    override fun setNotificationUsageAutoAddEnabled(filterId: String, value: Boolean) {
        prefs.edit { putBoolean(notificationUsageAutoAddEnabledKey(filterId), value) }
        notificationUsageAutoAddEnabledFlow.update { it + (filterId to value) }
    }

    private fun notificationUsageAutoAddEnabledKey(filterId: String): String {
        return "notificationUsageAutoAddEnabled.$filterId"
    }
}
