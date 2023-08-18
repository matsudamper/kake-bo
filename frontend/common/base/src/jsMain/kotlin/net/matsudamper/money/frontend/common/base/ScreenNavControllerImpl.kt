package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import io.ktor.http.ParametersBuilder
import io.ktor.util.StringValues
import io.ktor.util.toMap
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.Screens
import net.matsudamper.money.frontend.common.base.nav.user.UrlPlaceHolderParser

@Immutable
@Suppress("RegExpRedundantEscape")
public class ScreenNavControllerImpl(
    initial: ScreenStructure,
) : ScreenNavController<ScreenStructure> {
    private val directions = Screens.values().toList()
    private val parser = UrlPlaceHolderParser(directions)
    private var screenState: ScreenState by mutableStateOf(
        ScreenState(
            current = initial,
            lastHome = null,
        ),
    )
    override val currentNavigation: ScreenStructure
        get() {
            return screenState.current
        }

    init {
        updateScreenState(
            parser.parse(pathname = window.location.pathname)
                .toScreenStructure(parseQueryParams(window.location.search)),
        )

        window.addEventListener(
            "popstate",
            callback = {
                updateScreenState(
                    parser.parse(pathname = window.location.pathname)
                        .toScreenStructure(parseQueryParams(window.location.search)),
                )
            },
        )
    }

    override fun navigate(
        navigation: ScreenStructure,
    ) {
        val url = navigation.createUrl()
        if (screenState.current.equalScreen(navigation)) {
            window.history.replaceState(
                data = null,
                title = navigation.direction.title,
                url = url,
            )
        } else {
            window.history.pushState(
                data = null,
                title = navigation.direction.title,
                url = url,
            )
        }
        updateScreenState(navigation)
    }

    override fun navigateToHome() {
        navigate(
            screenState.lastHome ?: ScreenStructure.Root.HomeAnalytics(),
        )
    }

    private fun parseQueryParams(query: String): Map<String, List<String>> {
        return ParametersBuilder().apply {
            appendAll(
                StringValues.build {
                    query.removePrefix("?")
                        .split("&")
                        .forEach { keyValue ->
                            keyValue.split("=").let {
                                val key = it.getOrNull(0) ?: return@forEach
                                val value = it.getOrNull(1).orEmpty()

                                append(key, value)
                            }
                        }
                },
            )
        }.build().toMap()
    }

    private fun updateScreenState(screenStructure: ScreenStructure) {
        println("updateScreenState: $screenStructure, ${screenStructure.createUrl()}")
        screenState = screenState.copy(
            current = screenStructure,
            lastHome = when (screenStructure) {
                is ScreenStructure.Root -> screenStructure
                else -> screenState.lastHome
            },
        )
    }

    override fun back() {
        window.history.back()
    }

    private fun UrlPlaceHolderParser.ScreenState<Screens>.toScreenStructure(
        queryParams: Map<String, List<String>>,
    ): ScreenStructure {
        return when (this.screen) {
            Screens.Home, Screens.HomeRedirect -> ScreenStructure.Root.HomeAnalytics.create(
                pathParams = pathParams,
                queryParams = queryParams,
            )
            Screens.HomePeriodSubCategory -> ScreenStructure.Root.HomeSubCategory.create(
                pathParams = pathParams,
                queryParams = queryParams,
            )

            Screens.Settings -> ScreenStructure.Root.Settings.Root
            Screens.SettingsImap -> ScreenStructure.Root.Settings.Imap
            Screens.SettingsCategory -> ScreenStructure.Root.Settings.Categories
            Screens.SettingsCategoryId -> ScreenStructure.Root.Settings.Category(
                id = this.pathParams["id"]?.toIntOrNull()?.let { MoneyUsageCategoryId(it) }
                    ?: return ScreenStructure.NotFound,
            )

            Screens.UsageList -> ScreenStructure.Root.Usage.List()
            Screens.UsageCalendar -> ScreenStructure.Root.Usage.Calendar()
            Screens.Login -> ScreenStructure.Login
            Screens.Admin -> ScreenStructure.Admin
            Screens.MailImport -> ScreenStructure.Root.Mail.Import
            Screens.MailList -> ScreenStructure.Root.Mail.Imported.create(
                pathParams = pathParams,
                queryParams = queryParams,
            )

            Screens.AddMoneyUsage -> {
                ScreenStructure.AddMoneyUsage.fromQueryParams(
                    queryParams = queryParams,
                )
            }

            Screens.ImportedMail -> {
                ScreenStructure.ImportedMail(
                    id = run id@{
                        val id = this.pathParams["id"]?.toIntOrNull() ?: return ScreenStructure.NotFound
                        ImportedMailId(id)
                    },
                )
            }

            Screens.ImportedMailPlain -> {
                ScreenStructure.ImportedMailPlain(
                    id = run id@{
                        val id = this.pathParams["id"]?.toIntOrNull() ?: return ScreenStructure.NotFound
                        ImportedMailId(id)
                    },
                )
            }

            Screens.ImportedMailHTML -> {
                ScreenStructure.ImportedMailHTML(
                    id = run id@{
                        val id = this.pathParams["id"]?.toIntOrNull() ?: return ScreenStructure.NotFound
                        ImportedMailId(id)
                    },
                )
            }

            null,
            Screens.NotFound,
            -> ScreenStructure.NotFound

            Screens.MailCategoryFilters -> ScreenStructure.Root.Settings.MailCategoryFilters
            Screens.MailCategoryFilter -> ScreenStructure.Root.Settings.MailCategoryFilter(
                id = run id@{
                    val id = this.pathParams["id"]?.toIntOrNull() ?: return ScreenStructure.NotFound
                    ImportedMailCategoryFilterId(id)
                },
            )

            Screens.MoneyUsage -> {
                ScreenStructure.MoneyUsage(
                    id = run id@{
                        val id = this.pathParams["id"]?.toIntOrNull() ?: return ScreenStructure.NotFound
                        MoneyUsageId(id)
                    },
                )
            }
        }
    }

    public data class ScreenState(
        val current: ScreenStructure,
        val lastHome: ScreenStructure.Root?,
    )
}
