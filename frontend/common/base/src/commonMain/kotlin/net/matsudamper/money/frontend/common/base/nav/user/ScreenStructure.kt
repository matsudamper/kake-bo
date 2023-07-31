package net.matsudamper.money.frontend.common.base.nav.user

import io.ktor.http.ParametersBuilder
import io.ktor.http.formUrlEncode
import net.matsudamper.money.element.MoneyUsageCategoryId

public sealed interface ScreenStructure {
    public val direction: Screens

    public fun createUrl(): String {
        return direction.placeholderUrl
    }

    public sealed interface Root : ScreenStructure {
        public class Home : Root {
            override val direction: Screens = Screens.Home
        }

        public sealed interface Settings : Root {
            public object Root : Settings {
                override val direction: Screens = Screens.Settings
            }

            public object Imap : Settings {
                override val direction: Screens = Screens.SettingsImap
            }

            public object Categories : Settings {
                override val direction: Screens = Screens.SettingsCategory
            }

            public class Category(
                public val id: MoneyUsageCategoryId,
            ) : Settings {
                override val direction: Screens = Screens.SettingsCategoryId

                override fun createUrl(): String {
                    return direction.placeholderUrl.replace("{id}", id.id.toString())
                }
            }

            public object SubCategory : Settings {
                override val direction: Screens = Screens.SettingsSubCategory
            }

            public class SubCategoryId(
                public val id: Int,
            ) : Settings {
                override val direction: Screens = Screens.SettingsSubCategoryId

                override fun createUrl(): String {
                    return direction.placeholderUrl.replace("{id}", id.toString())
                }
            }
        }

        public class List : Root {
            override val direction: Screens = Screens.List
        }
    }

    public object NotFound : ScreenStructure {
        override val direction: Screens = Screens.NotFound
    }

    public object Login : ScreenStructure {
        override val direction: Screens = Screens.Login
    }

    public object Admin : ScreenStructure {
        override val direction: Screens = Screens.Admin
    }

    public object MailImport : ScreenStructure {
        override val direction: Screens = Screens.MailImport
    }

    public class MailList(
        public val isLinked: Boolean?,
    ) : ScreenStructure {
        override val direction: Screens = Screens.MailList

        override fun createUrl(): String {
            val urlParam = ParametersBuilder()
                .apply {
                    if (isLinked != null) {
                        append(KEY_IS_LINKED, isLinked.toString())
                    }
                }
                .build()
                .formUrlEncode()
                .let {
                    if (it.isEmpty()) {
                        it
                    } else {
                        "?$it"
                    }
                }

            return direction.placeholderUrl.plus(urlParam)
        }

        public companion object {
            private const val KEY_IS_LINKED = "is_linked"

            @Suppress("UNUSED_PARAMETER")
            public fun create(
                pathParams: Map<String, String>,
                queryParams: Map<String, List<String>>,
            ): MailList {
                return MailList(
                    isLinked = queryParams[KEY_IS_LINKED]
                        ?.firstOrNull()
                        ?.toBooleanStrictOrNull(),
                )
            }
        }
    }

    public object AddMoneyUsage : ScreenStructure {
        override val direction: Screens = Screens.AddMoneyUsage
    }
}
