package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import net.matsudamper.money.frontend.common.base.nav.user.RootTab
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

    override fun changeTab(tab: RootTab) {
        val state = when (tab) {
            RootTab.Home -> screenState.home ?: ScreenStructure.Root.Home()
            RootTab.Register -> screenState.register ?: ScreenStructure.Root.Register()
            RootTab.Settings -> screenState.settings ?: ScreenStructure.Root.Settings.Root
        }
        if (screenState.current == state) return

        val url = state.createUrl()
        window.history.pushState(
            data = null,
            title = state.direction.title,
            url = url,
        )
        updateScreenState(state)
    }

    private fun updateScreenState(screenStructure: ScreenStructure) {
        println("updateScreenState: $screenStructure, ${screenStructure.createUrl()}")
        screenState = screenState.copy(
            current = screenStructure,
            home = when (screenStructure) {
                is ScreenStructure.Root.Home -> screenStructure
                else -> screenState.home
            },
            settings = when (screenStructure) {
                is ScreenStructure.Root.Settings -> screenStructure
                else -> screenState.settings
            },
            register = when (screenStructure) {
                is ScreenStructure.Root.Register -> screenStructure
                else -> screenState.register
            },
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
            Screens.SettingsCategory -> ScreenStructure.Root.Settings.Category
            Screens.SettingsCategoryId -> ScreenStructure.Root.Settings.CategoryId
            Screens.SettingsSubCategory -> ScreenStructure.Root.Settings.SubCategory
            Screens.SettingsSubCategoryId -> {
                ScreenStructure.Root.Settings.SubCategoryId(
                    id = this.params["id"]?.toIntOrNull() ?: return ScreenStructure.NotFound,
                )
            }

            Screens.Register -> ScreenStructure.Root.Register()
            Screens.NotFound -> ScreenStructure.NotFound
            Screens.Login -> ScreenStructure.Login
            Screens.Admin -> ScreenStructure.Admin
            Screens.MailImport -> ScreenStructure.MailImport
            Screens.MailLink -> ScreenStructure.MailLink
        }
    }

    public data class ScreenState(
        val current: ScreenStructure,
        val home: ScreenStructure? = null,
        val settings: ScreenStructure? = null,
        val register: ScreenStructure? = null,
    )
}

