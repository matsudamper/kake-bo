package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.Screens
import net.matsudamper.money.frontend.common.base.nav.user.UrlPlaceHolderParser

@Immutable
@Suppress("RegExpRedundantEscape")
public class ScreenNavControllerImpl(
    initial: ScreenStructure,
) : ScreenNavController {
    private val directions = Screens.values().toList()
    private val parser = UrlPlaceHolderParser(directions)
    private var screenState: ScreenState by mutableStateOf(ScreenState(current = initial))
    override val currentNavigation: ScreenStructure
        get() {
            return screenState.current
        }

    init {
        updateScreenState(
            parser.parse(pathname = window.location.pathname)
                .toScreenStructure(),
        )

        window.addEventListener(
            "popstate",
            callback = {
                updateScreenState(
                    parser.parse(pathname = window.location.pathname)
                        .toScreenStructure(),
                )
            },
        )
    }

    override fun <T : ScreenStructure> navigate(
        navigation: T,
    ) {
        val url = navigation.createUrl()
        window.history.pushState(
            data = null,
            title = navigation.direction.title,
            url = url,
        )
        updateScreenState(navigation)
    }

    private fun updateScreenState(screenStructure: ScreenStructure) {
        println("updateScreenState: $screenStructure, ${screenStructure.createUrl()}")
        screenState = screenState.copy(
            current = screenStructure,
        )
    }

    override fun back() {
        window.history.back()
    }

    private fun UrlPlaceHolderParser.ScreenState.toScreenStructure(): ScreenStructure {
        return when (this.screen) {
            Screens.Home -> ScreenStructure.Root.Home()
            Screens.Settings -> ScreenStructure.Root.Settings.Root
            Screens.SettingsImap -> ScreenStructure.Root.Settings.Imap
            Screens.SettingsCategory -> ScreenStructure.Root.Settings.Categories
            Screens.SettingsCategoryId -> ScreenStructure.Root.Settings.Category(
                id = this.params["id"]?.toIntOrNull()?.let { MoneyUsageCategoryId(it) } ?: return ScreenStructure.NotFound,
            )
            Screens.SettingsSubCategory -> ScreenStructure.Root.Settings.SubCategory
            Screens.SettingsSubCategoryId -> {
                ScreenStructure.Root.Settings.SubCategoryId(
                    id = this.params["id"]?.toIntOrNull() ?: return ScreenStructure.NotFound,
                )
            }

            Screens.List -> ScreenStructure.Root.List()
            Screens.NotFound -> ScreenStructure.NotFound
            Screens.Login -> ScreenStructure.Login
            Screens.Admin -> ScreenStructure.Admin
            Screens.MailImport -> ScreenStructure.MailImport
            Screens.MailLink -> ScreenStructure.MailLink
            Screens.AddMoneyUsage -> ScreenStructure.AddMoneyUsage
        }
    }

    public data class ScreenState(
        val current: ScreenStructure,
    )
}
