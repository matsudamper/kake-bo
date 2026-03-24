package net.matsudamper.money.frontend.common.base

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

public class AppSettingsRepositoryJsImpl : AppSettingsRepository {
    private val showImagesKey = "showImagesInMonthlyScreen"

    private val _showImagesInMonthlyScreen = MutableStateFlow(
        localStorage.getItem(showImagesKey)?.toBoolean() ?: false,
    )

    override val showImagesInMonthlyScreen: Flow<Boolean> = _showImagesInMonthlyScreen

    override fun setShowImagesInMonthlyScreen(value: Boolean) {
        localStorage.setItem(showImagesKey, value.toString())
        _showImagesInMonthlyScreen.value = value
    }
}
