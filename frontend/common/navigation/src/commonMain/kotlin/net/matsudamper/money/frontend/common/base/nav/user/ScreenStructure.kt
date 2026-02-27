package net.matsudamper.money.frontend.common.base.nav.user

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId

public sealed interface ScreenStructure : IScreenStructure {
    public sealed interface Root : ScreenStructure {
        public sealed interface Settings : Root {
            override val stackGroupId: String get() = "ScreenStructure#Root#Settings"

            @Serializable
            public data object Root : Settings {
                override val direction: Screens = Screens.Settings
                override val sameScreenId: String = "ScreenStructure#Root#Settings#Root"
            }

            @Serializable
            public data object Api : Settings {
                override val direction: Screens = Screens.Api
                override val sameScreenId: String = "ScreenStructure#Root#Settings#Api"
            }

            @Serializable
            public data object Login : Settings {
                override val direction: Screens = Screens.SettingsLogin
                override val sameScreenId: String = "ScreenStructure#Root#Settings#Login"
            }

            @Serializable
            public data object Imap : Settings {
                override val direction: Screens = Screens.SettingsImap
                override val sameScreenId: String = "ScreenStructure#Root#Settings#Imap"
            }

            @Serializable
            public data object Categories : Settings {
                override val direction: Screens = Screens.SettingsCategory
                override val sameScreenId: String = "ScreenStructure#Root#Settings#Categories"
            }

            @Serializable
            public data class Category(
                public val id: MoneyUsageCategoryId,
            ) : Settings {
                override val direction: Screens = Screens.SettingsCategoryId
                override val sameScreenId: String = "ScreenStructure#Root#Settings#Category"

                override fun createUrl(): String {
                    return direction.placeholderUrl.replace("{id}", id.value.toString())
                }
            }

            @Serializable
            public data object TextFieldTest : Settings {
                override val direction: Screens = Screens.SettingsTextFieldTest
                override val sameScreenId: String = "ScreenStructure#Root#Settings#TextFieldTest"
            }

            @Serializable
            public data object MailCategoryFilters : Settings {
                override val direction: Screens = Screens.MailCategoryFilters
                override val sameScreenId: String = "ScreenStructure#Root#Settings#MailCategoryFilters"
            }

            @Serializable
            public data class MailCategoryFilter(
                val id: ImportedMailCategoryFilterId,
            ) : Settings {
                override val direction: Screens = Screens.MailCategoryFilter
                override val sameScreenId: String = "ScreenStructure#Root#Settings#MailCategoryFilter($id)"

                override fun createUrl(): String {
                    return direction.placeholderUrl.replace("{id}", id.id.toString())
                }
            }
        }

        public sealed interface Add : Root {
            override val stackGroupId: String get() = "ScreenStructure#Root#Add"

            @Serializable
            public data object Root : Add {
                override val direction: Screens = Screens.Add
                override val sameScreenId: String = "ScreenStructure#Root#Add#Root"
            }

            @Serializable
            public data class Imported(
                public val isLinked: Boolean?,
                public val text: String?,
            ) : Add {
                override val direction: Screens = Screens.ImportedMailList
                override val sameScreenId: String = "ScreenStructure#Root#Add#Imported"

                override fun createUrl(): String {
                    val urlParam = buildParameter {
                        if (isLinked != null) {
                            append(KEY_IS_LINKED, isLinked.toString())
                        }
                        if (text != null) {
                            append(KEY_TEXT, text)
                        }
                    }

                    return direction.placeholderUrl.plus(urlParam)
                }

                public companion object {
                    private const val KEY_IS_LINKED = "is_linked"
                    private const val KEY_TEXT = "text"

                    @Suppress("UNUSED_PARAMETER")
                    public fun create(
                        pathParams: Map<String, String>,
                        queryParams: Map<String, List<String>>,
                    ): Imported {
                        return Imported(
                            isLinked = queryParams[KEY_IS_LINKED]
                                ?.firstOrNull()
                                ?.toBooleanStrictOrNull(),
                            text = queryParams[KEY_TEXT]
                                ?.firstOrNull(),
                        )
                    }
                }
            }

            @Serializable
            public data object Import : Add {
                override val direction: Screens = Screens.MailImport
                override val sameScreenId: String = "ScreenStructure#Root#Add#Import"
            }

            @Serializable
            public data object Recurring : Add {
                override val direction: Screens = Screens.AddRecurringUsage
                override val sameScreenId: String = "ScreenStructure#Root#Add#Recurring"
            }
        }

        public sealed interface Usage : Root {
            override val stackGroupId: String get() = "ScreenStructure#Root#Usage"

            @Serializable
            public data object List : Usage {
                override val direction: Screens = Screens.UsageList
                override val sameScreenId: String = "ScreenStructure#Root#Usage#List"
            }

            @Serializable
            public data class Calendar(
                val yearMonth: YearMonth? = null,
            ) : Usage {
                override val direction: Screens = Screens.UsageCalendar
                override val sameScreenId: String = "ScreenStructure#Root#Usage#Calendar"

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

                @Serializable
                public data class YearMonth(
                    val year: Int,
                    val month: Int,
                )

                public companion object {
                    public fun fromQueryParams(queryParams: Map<String, kotlin.collections.List<String>>): Calendar {
                        val year = queryParams["year"]?.firstOrNull()?.toIntOrNull()
                            ?: return Calendar()
                        val month = queryParams["month"]?.firstOrNull()?.toIntOrNull()
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

    @Serializable
    public data class CalendarDateList(
        val year: Int,
        val month: Int,
        val day: Int,
    ) : ScreenStructure {
        override val direction: Screens = Screens.CalendarDateList
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#CalendarDateList($year-$month-$day)"

        override fun createUrl(): String {
            return direction.placeholderUrl.plus(
                buildParameter {
                    append(KEY_YEAR, year.toString())
                    append(KEY_MONTH, month.toString())
                    append(KEY_DAY, day.toString())
                },
            )
        }

        public companion object {
            private const val KEY_YEAR = "year"
            private const val KEY_MONTH = "month"
            private const val KEY_DAY = "day"

            public fun fromQueryParams(queryParams: Map<String, List<String>>): CalendarDateList? {
                val year = queryParams[KEY_YEAR]?.firstOrNull()?.toIntOrNull() ?: return null
                val month = queryParams[KEY_MONTH]?.firstOrNull()?.toIntOrNull() ?: return null
                val day = queryParams[KEY_DAY]?.firstOrNull()?.toIntOrNull() ?: return null
                return CalendarDateList(
                    year = year,
                    month = month,
                    day = day,
                )
            }
        }
    }

    @Serializable
    public data object NotFound : ScreenStructure {
        override val direction: Screens = Screens.NotFound
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#NotFound"
    }

    @Serializable
    public data object Login : ScreenStructure {
        override val direction: Screens = Screens.Login
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#Login"
    }

    @Serializable
    public data object Admin : ScreenStructure {
        override val direction: Screens = Screens.Admin
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#Admin"
    }

    @Serializable
    public data object Splash : ScreenStructure {
        override val direction: Screens = Screens.Splash
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#Splash"
    }

    @Serializable
    public data class ImportedMail(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMail
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#ImportedMail($id)"

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }
    }

    @Serializable
    public data class MoneyUsage(
        public val id: MoneyUsageId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.MoneyUsage
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#MoneyUsage($id)"

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }
    }

    @Serializable
    public data class ImportedMailHTML(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMailHTML
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#ImportedMailHTML($id)"

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }
    }

    @Serializable
    public data class ImportedMailPlain(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMailPlain
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#ImportedMailPlain($id)"

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }
    }

    @Serializable
    public data class AddMoneyUsage(
        val importedMailId: ImportedMailId? = null,
        val importedMailIndex: Int? = null,
        val title: String? = null,
        val price: Float? = null,
        val date: LocalDateTime? = null,
        val description: String? = null,
        val subCategoryId: String? = null,
    ) : ScreenStructure {
        override val direction: Screens = Screens.AddMoneyUsage
        override val stackGroupId: String? = null
        override val sameScreenId: String = "ScreenStructure#AddMoneyUsage"

        override fun createUrl(): String {
            return direction.placeholderUrl.plus(
                buildParameter {
                    if (importedMailId != null) {
                        append(KEY_IMPORTED_MAIL_ID, importedMailId.id.toString())
                    }
                    if (importedMailIndex != null) {
                        append(KEY_IMPORTED_MAIL_INDEX, importedMailIndex.toString())
                    }
                    if (title != null) {
                        append(KEY_TITLE, title)
                    }
                    if (description != null) {
                        append(KEY_DESCRIPTION, description)
                    }
                },
            )
        }

        public companion object {
            private const val KEY_IMPORTED_MAIL_ID = "imported_mail_id"
            private const val KEY_IMPORTED_MAIL_INDEX = "imported_mail_index"
            private const val KEY_TITLE = "title"
            private const val KEY_PRICE = "price"
            private const val KEY_DATE = "date"
            private const val KEY_DESCRIPTION = "description"
            private const val KEY_SUB_CATEGORY_ID = "sub_category_id"

            public fun fromQueryParams(queryParams: Map<String, List<String>>): AddMoneyUsage {
                return AddMoneyUsage(
                    importedMailId = queryParams[KEY_IMPORTED_MAIL_ID]?.firstOrNull()?.toIntOrNull()
                        ?.let { ImportedMailId(it) },
                    importedMailIndex = queryParams[KEY_IMPORTED_MAIL_INDEX]?.firstOrNull()?.toIntOrNull(),
                    title = queryParams[KEY_TITLE]?.firstOrNull(),
                    price = queryParams[KEY_PRICE]?.firstOrNull()?.toFloatOrNull(),
                    date = queryParams[KEY_DATE]?.firstOrNull()?.let { LocalDateTime.parse(it) },
                    description = queryParams[KEY_DESCRIPTION]?.firstOrNull(),
                    subCategoryId = queryParams[KEY_SUB_CATEGORY_ID]?.firstOrNull(),
                )
            }
        }
    }
}
