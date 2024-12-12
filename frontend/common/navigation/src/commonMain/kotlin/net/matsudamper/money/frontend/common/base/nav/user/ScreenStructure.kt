package net.matsudamper.money.frontend.common.base.nav.user

import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId

public sealed interface ScreenStructure : IScreenStructure {
    public sealed interface Root : ScreenStructure {
        public sealed interface Settings : Root {
            override val groupId: Any get() = Settings::class
            public data object Root : Settings {
                override val direction: Screens = Screens.Settings
            }

            public data object Api : Settings {
                override val direction: Screens = Screens.Api
            }

            public data object Login : Settings {
                override val direction: Screens = Screens.SettingsLogin
            }

            public data object Imap : Settings {
                override val direction: Screens = Screens.SettingsImap
            }

            public data object Categories : Settings {
                override val direction: Screens = Screens.SettingsCategory
            }

            public data class Category(
                public val id: MoneyUsageCategoryId,
            ) : Settings {
                override val direction: Screens = Screens.SettingsCategoryId

                override fun createUrl(): String {
                    return direction.placeholderUrl.replace("{id}", id.value.toString())
                }
            }

            public data object MailCategoryFilters : Settings {
                override val direction: Screens = Screens.MailCategoryFilters
            }

            public data class MailCategoryFilter(
                val id: ImportedMailCategoryFilterId,
            ) : Settings {
                override val direction: Screens = Screens.MailCategoryFilter

                override fun createUrl(): String {
                    return direction.placeholderUrl.replace("{id}", id.id.toString())
                }
            }
        }

        public sealed interface Add : Root {
            override val groupId: Any get() = Add::class
            public data object Root : Add {
                override val direction: Screens = Screens.Add
            }

            public data class Imported(
                public val isLinked: Boolean?,
            ) : Add {
                override val direction: Screens = Screens.ImportedMailList

                override fun createUrl(): String {
                    val urlParam =
                        buildParameter {
                            if (isLinked != null) {
                                append(KEY_IS_LINKED, isLinked.toString())
                            }
                        }

                    return direction.placeholderUrl.plus(urlParam)
                }

                override fun equalScreen(other: IScreenStructure): Boolean {
                    return other is Imported
                }

                public companion object {
                    private const val KEY_IS_LINKED = "is_linked"

                    @Suppress("UNUSED_PARAMETER")
                    public fun create(
                        pathParams: Map<String, String>,
                        queryParams: Map<String, List<String>>,
                    ): Imported {
                        return Imported(
                            isLinked =
                            queryParams[KEY_IS_LINKED]
                                ?.firstOrNull()
                                ?.toBooleanStrictOrNull(),
                        )
                    }
                }
            }

            public data object Import : Add {
                override val direction: Screens = Screens.MailImport
            }
        }

        public sealed interface Usage : Root {
            override val groupId: Any get() = Usage::class
            public data object List : Usage {
                override val direction: Screens = Screens.UsageList

                override fun equalScreen(other: IScreenStructure): Boolean {
                    return other is List
                }
            }

            public data class Calendar(
                val yearMonth: YearMonth? = null,
            ) : Usage {
                override val direction: Screens = Screens.UsageCalendar

                override fun equalScreen(other: IScreenStructure): Boolean {
                    return other is Calendar
                }

                override fun createUrl(): String {
                    return if (yearMonth == null) {
                        direction.placeholderUrl
                    } else {
                        buildString {
                            append(direction.placeholderUrl)
                            append("?")
                            append("year=${yearMonth.year}&month=${yearMonth.month}")
                        }
                    }
                }

                public data class YearMonth(
                    val year: Int,
                    val month: Int,
                )

                public companion object {
                    public fun fromQueryParams(queryParams: Map<String, kotlin.collections.List<String>>): Calendar {
                        val year =
                            queryParams["year"]?.firstOrNull()?.toIntOrNull()
                                ?: return Calendar()
                        val month =
                            queryParams["month"]?.firstOrNull()?.toIntOrNull()
                                ?: return Calendar()
                        return Calendar(
                            YearMonth(
                                year = year,
                                month = month,
                            ),
                        )
                    }
                }
            }
        }
    }

    public data object NotFound : ScreenStructure {
        override val direction: Screens = Screens.NotFound
        override val groupId: Any? = null
    }

    public data object Login : ScreenStructure {
        override val direction: Screens = Screens.Login
        override val groupId: Any? = null
    }

    public data object Admin : ScreenStructure {
        override val direction: Screens = Screens.Admin
        override val groupId: Any? = null
    }

    public data class ImportedMail(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMail
        override val groupId: Any? = null

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }

        override fun equalScreen(other: IScreenStructure): Boolean {
            return this == other
        }
    }

    public data class MoneyUsage(
        public val id: MoneyUsageId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.MoneyUsage
        override val groupId: Any? = null

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }

        override fun equalScreen(other: IScreenStructure): Boolean {
            return this == other
        }
    }

    public data class ImportedMailHTML(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMailHTML
        override val groupId: Any? = null

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }

        override fun equalScreen(other: IScreenStructure): Boolean {
            return this == other
        }
    }

    public data class ImportedMailPlain(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMailPlain
        override val groupId: Any? = null

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }

        override fun equalScreen(other: IScreenStructure): Boolean {
            return this == other
        }
    }

    public data class AddMoneyUsage(
        val importedMailId: ImportedMailId? = null,
        val importedMailIndex: Int? = null,
    ) : ScreenStructure {
        override val direction: Screens = Screens.AddMoneyUsage
        override val groupId: Any? = null

        override fun createUrl(): String {
            return direction.placeholderUrl.plus(
                buildParameter {
                    if (importedMailId != null) {
                        append(IMPORTED_MAIL_ID, importedMailId.id.toString())
                    }
                    if (importedMailIndex != null) {
                        append(IMPORTED_MAIL_INDEX, importedMailIndex.toString())
                    }
                },
            )
        }

        override fun equalScreen(other: IScreenStructure): Boolean {
            return other is AddMoneyUsage
        }

        public companion object {
            private const val IMPORTED_MAIL_ID = "imported_mail_id"
            private const val IMPORTED_MAIL_INDEX = "imported_mail_index"

            public fun fromQueryParams(queryParams: Map<String, List<String>>): AddMoneyUsage {
                return AddMoneyUsage(
                    importedMailId =
                    queryParams[IMPORTED_MAIL_ID]?.firstOrNull()?.toIntOrNull()
                        ?.let { ImportedMailId(it) },
                    importedMailIndex = queryParams[IMPORTED_MAIL_INDEX]?.firstOrNull()?.toIntOrNull(),
                )
            }
        }
    }
}
