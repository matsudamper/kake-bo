package net.matsudamper.money.frontend.common.base.nav.user

import io.ktor.http.ParametersBuilder
import io.ktor.http.formUrlEncode
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId

public sealed interface ScreenStructure : IScreenStructure<ScreenStructure> {
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

            public object MailCategoryFilters : Settings {
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

        public sealed interface Mail : Root {
            public class Imported(
                public val isLinked: Boolean?,
            ) : Mail {
                override val direction: Screens = Screens.MailList

                override fun createUrl(): String {
                    val urlParam = buildParameter {
                        if (isLinked != null) {
                            append(KEY_IS_LINKED, isLinked.toString())
                        }
                    }

                    return direction.placeholderUrl.plus(urlParam)
                }

                override fun equalScreen(other: ScreenStructure): Boolean {
                    return other is Imported
                }

                public companion object {
                    private const val KEY_IS_LINKED = "is_linked"

                    @Suppress("UNUSED_PARAMETER")
                    public fun create(
                        pathParams: Map<String, String>,
                        queryParams: Map<String, kotlin.collections.List<String>>,
                    ): Imported {
                        return Imported(
                            isLinked = queryParams[KEY_IS_LINKED]
                                ?.firstOrNull()
                                ?.toBooleanStrictOrNull(),
                        )
                    }
                }
            }

            public object Import : Mail {
                override val direction: Screens = Screens.MailImport
            }
        }

        public sealed interface Usage : Root {
            public class List : Usage {
                override val direction: Screens = Screens.UsageList
                override fun equalScreen(other: ScreenStructure): Boolean {
                    return other is List
                }
            }

            public class Calendar : Usage {
                override val direction: Screens = Screens.UsageCalendar
                override fun equalScreen(other: ScreenStructure): Boolean {
                    return other is Calendar
                }
            }
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

    public data class ImportedMail(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMail

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }

        override fun equalScreen(other: ScreenStructure): Boolean {
            return this == other
        }
    }

    public data class MoneyUsage(
        public val id: MoneyUsageId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.MoneyUsage

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }

        override fun equalScreen(other: ScreenStructure): Boolean {
            return this == other
        }
    }

    public data class ImportedMailHTML(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMailHTML

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }

        override fun equalScreen(other: ScreenStructure): Boolean {
            return this == other
        }
    }

    public data class ImportedMailPlain(
        public val id: ImportedMailId,
    ) : ScreenStructure {
        override val direction: Screens = Screens.ImportedMailPlain

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{id}", id.id.toString())
        }

        override fun equalScreen(other: ScreenStructure): Boolean {
            return this == other
        }
    }

    public data class AddMoneyUsage(
        val importedMailId: ImportedMailId? = null,
        val importedMailIndex: Int? = null,
    ) : ScreenStructure {
        override val direction: Screens = Screens.AddMoneyUsage

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

        override fun equalScreen(other: ScreenStructure): Boolean {
            return other is AddMoneyUsage
        }

        public companion object {
            private const val IMPORTED_MAIL_ID = "imported_mail_id"
            private const val IMPORTED_MAIL_INDEX = "imported_mail_index"

            public fun fromQueryParams(queryParams: Map<String, List<String>>): AddMoneyUsage {
                return AddMoneyUsage(
                    importedMailId = queryParams[IMPORTED_MAIL_ID]?.firstOrNull()?.toIntOrNull()
                        ?.let { ImportedMailId(it) },
                    importedMailIndex = queryParams[IMPORTED_MAIL_INDEX]?.firstOrNull()?.toIntOrNull(),
                )
            }
        }
    }
}

private fun buildParameter(block: ParametersBuilder.() -> Unit): String {
    return ParametersBuilder()
        .apply(block)
        .build()
        .formUrlEncode()
        .let { if (it.isEmpty()) it else "?$it" }
}
