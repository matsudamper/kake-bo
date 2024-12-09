package net.matsudamper.money.frontend.common.base.nav.user

import kotlinx.datetime.LocalDate
import net.matsudamper.money.element.MoneyUsageCategoryId

public sealed interface RootHomeScreenStructure : ScreenStructure.Root {
    public sealed interface Period : RootHomeScreenStructure {
        public val since: LocalDate?
        public val period: Int
    }

    public data object Home : Period {
        override val since: LocalDate? = null
        override val period: Int = 3
        override val direction: Direction = Screens.HomeRedirect
    }

    public data class PeriodAnalytics(
        override val since: LocalDate? = null,
        override val period: Int = 3,
    ) : Period {
        override val direction: Screens = Screens.Home

        override fun createUrl(): String {
            val since = since
            val urlParam =
                buildParameter {
                    if (since != null) {
                        append(
                            SINCE_KEY,
                            buildString {
                                append(since.year)
                                append("-")
                                append(since.monthNumber.toString().padStart(2, '0'))
                            },
                        )
                        append(
                            PERIOD_KEY,
                            period.toString(),
                        )
                    }
                }
            return direction.placeholderUrl.plus(urlParam)
        }

        override fun equalScreen(other: IScreenStructure): Boolean {
            return other is PeriodAnalytics
        }

        public companion object {
            private const val SINCE_KEY = "since"
            private const val PERIOD_KEY = "period"

            @Suppress("UNUSED_PARAMETER")
            public fun create(
                pathParams: Map<String, String>,
                queryParams: Map<String, List<String>>,
            ): PeriodAnalytics {
                return PeriodAnalytics(
                    since =
                    queryParams[SINCE_KEY]?.firstOrNull()
                        ?.let { LocalDate.parse("$it-01") },
                    period = queryParams[PERIOD_KEY]?.firstOrNull()?.toIntOrNull() ?: 3,
                )
            }
        }
    }

    public data class PeriodCategory(
        val categoryId: MoneyUsageCategoryId,
        override val period: Int = 3,
        override val since: LocalDate? = null,
    ) : Period {
        override val direction: Screens = Screens.HomePeriodCategory

        override fun createUrl(): String {
            val urlParam =
                buildParameter {
                    if (since != null) {
                        append(
                            SINCE_KEY,
                            buildString {
                                append(since.year)
                                append("-")
                                append(since.monthNumber.toString().padStart(2, '0'))
                            },
                        )
                    }
                }
            return direction.placeholderUrl
                .replace("{id}", categoryId.value.toString())
                .plus(urlParam)
        }

        override fun equalScreen(other: IScreenStructure): Boolean {
            return other is PeriodAnalytics
        }

        public companion object {
            private const val SINCE_KEY = "since"

            public fun create(
                pathParams: Map<String, String>,
                queryParams: Map<String, List<String>>,
            ): PeriodCategory {
                return PeriodCategory(
                    categoryId = MoneyUsageCategoryId(pathParams["id"]!!.toInt()),
                    since =
                    queryParams[SINCE_KEY]?.firstOrNull()
                        ?.let { LocalDate.parse("$it-01") },
                )
            }
        }
    }

    public data class Monthly(
        val date: LocalDate? = null,
    ) : RootHomeScreenStructure {
        override val direction: Screens = Screens.HomeMonthly

        override fun createUrl(): String {
            val urlParam =
                buildParameter {
                    if (date != null) {
                        append(
                            MONTH_KEY,
                            buildString {
                                append(date.year)
                                append("-")
                                append(date.monthNumber.toString().padStart(2, '0'))
                            },
                        )
                    }
                }
            return direction.placeholderUrl
                .plus(urlParam)
        }

        public companion object {
            private const val MONTH_KEY = "month"

            @Suppress("UNUSED_PARAMETER")
            public fun create(
                pathParams: Map<String, String>,
                queryParams: Map<String, List<String>>,
            ): Monthly {
                return Monthly(
                    date =
                    queryParams[MONTH_KEY]?.firstOrNull()
                        ?.let { LocalDate.parse("$it-01") },
                )
            }
        }
    }

    public data class MonthlyCategory(
        val categoryId: MoneyUsageCategoryId,
        val year: Int,
        val month: Int,
    ) : RootHomeScreenStructure {
        override val direction: Screens = Screens.HomeMonthlyCategory

        override fun createUrl(): String {
            return direction.placeholderUrl
                .replace("{$MONTH_KEY}", "$year-$month")
                .replace("{$CATEGORY_KEY}", categoryId.value.toString())
        }

        override fun equalScreen(other: IScreenStructure): Boolean {
            return other is MonthlyCategory
        }

        public companion object {
            private const val MONTH_KEY = "year-month"
            private const val CATEGORY_KEY = "id"

            @Suppress("UNUSED_PARAMETER")
            public fun create(
                pathParams: Map<String, String>,
                queryParams: Map<String, List<String>>,
            ): MonthlyCategory? {
                val category =
                    pathParams[CATEGORY_KEY]
                        ?.toIntOrNull()
                        ?: return null
                val year: Int
                val month: Int
                run {
                    val list =
                        pathParams[MONTH_KEY]
                            ?.split("-")
                            ?: return null

                    year = list.getOrNull(0)?.toIntOrNull() ?: return null
                    month = list.getOrNull(1)?.toIntOrNull() ?: return null
                }
                return MonthlyCategory(
                    year = year,
                    month = month,
                    categoryId = MoneyUsageCategoryId(category),
                )
            }
        }
    }
}
