package net.matsudamper.money.frontend.common.base

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

public class AppSettingsRepositoryAndroidImpl(context: Context) : AppSettingsRepository {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val showImagesKey = "showImagesInMonthlyScreen"

    private val _showImagesInMonthlyScreen = MutableStateFlow(
        prefs.getBoolean(showImagesKey, false),
    )

    override val showImagesInMonthlyScreen: Flow<Boolean> = _showImagesInMonthlyScreen

    override fun setShowImagesInMonthlyScreen(value: Boolean) {
        prefs.edit().putBoolean(showImagesKey, value).apply()
        _showImagesInMonthlyScreen.value = value
    }
}
