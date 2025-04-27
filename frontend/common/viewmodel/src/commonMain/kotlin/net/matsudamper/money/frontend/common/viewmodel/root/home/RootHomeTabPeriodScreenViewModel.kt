package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAndCategoryUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenQuery

public class RootHomeTabPeriodScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val api: RootHomeTabScreenApi,
    initialCategoryId: MoneyUsageCategoryId?,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            categoryId = initialCategoryId,
        ),
    )

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val event = object : RootHomeTabPeriodAndCategoryUiState.Event {
        override fun onClickNextMonth() {
            viewModelStateFlow.update {
                val period = viewModelStateFlow.value.displayPeriod
                it.copy(
                    displayPeriod = period.copy(
                        sinceDate = period.sinceDate.addMonth(1),
                    ),
                )
            }
            updateSinceDate()
        }

        override fun onClickPreviousMonth() {
            viewModelStateFlow.update {
                val period = viewModelStateFlow.value.displayPeriod
                it.copy(
                    displayPeriod = period.copy(
                        sinceDate = period.sinceDate.addMonth(-1),
                    ),
                )
            }
            updateSinceDate()
        }

        override fun onClickRange(range: Int) {
            val currentEndYearMonth = viewModelStateFlow.value.displayPeriod.let { period ->
                period.sinceDate.addMonth(period.monthCount)
            }
            val newSinceDate = currentEndYearMonth
                .addMonth(-range)

            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = ViewModelState.Period(
                        sinceDate = newSinceDate,
                        monthCount = range,
                    ),
                )
            }
            updateSinceDate()
        }

        override fun onViewInitialized() {
            collectScreenInfo()
        }

        // TODO Remove
        override fun onClickRetry() {
        }
    }

    public val uiStateFlow: StateFlow<RootHomeTabPeriodAndCategoryUiState> = MutableStateFlow(
        RootHomeTabPeriodAndCategoryUiState(
            loadingState = RootHomeTabPeriodAndCategoryUiState.LoadingState.Loading,
            event = event,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val loadingState = run screenState@{
                    val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                        viewModelState.displayPeriod.sinceDate.addMonth(index)
                    }

                    RootHomeTabPeriodAndCategoryUiState.LoadingState.Loaded(
                        categoryType = when (viewModelState.contentType) {
                            is ViewModelState.ContentType.All -> "すべて"
                            is ViewModelState.ContentType.Category -> viewModelState.contentType.name
                            is ViewModelState.ContentType.Loading -> ""
                        },
                        categoryTypes = createCategoryTypes(categories = viewModelState.categories).toImmutableList(),
                        between = "${displayPeriods.first().year}/${displayPeriods.first().month} - ${displayPeriods.last().year}/${displayPeriods.last().month}",
                        rangeText = "${viewModelState.displayPeriod.monthCount}ヶ月",
                    )
                }

                uiStateFlow.value = RootHomeTabPeriodAndCategoryUiState(
                    loadingState = loadingState,
                    event = event,
                )
            }
        }
    }.asStateFlow()

    public fun updateScreenStructure(current: RootHomeScreenStructure.Period) {
        val since = current.since
        if (since != null) {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    displayPeriod = viewModelState.displayPeriod.copy(
                        sinceDate = ViewModelState.YearMonth(
                            year = since.year,
                            month = since.monthNumber,
                        ),
                    ),
                )
            }
        }
    }

    public fun getCurrentLocalDate(): LocalDate {
        return LocalDate(
            year = viewModelStateFlow.value.displayPeriod.sinceDate.year,
            monthNumber = viewModelStateFlow.value.displayPeriod.sinceDate.month,
            dayOfMonth = 1,
        )
    }

    private fun updateSinceDate() {
        viewModelScope.launch {
            val newPeriod = viewModelStateFlow.value.displayPeriod
            viewModelEventSender.send {
                it.updateSinceDate(
                    year = newPeriod.sinceDate.year,
                    month = newPeriod.sinceDate.month,
                    period = newPeriod.monthCount,
                )
            }
        }
    }

    private fun collectScreenInfo() {
        viewModelScope.launch {
            api.screenFlow().collectLatest { response ->
                val newCategories = response.data?.user?.moneyUsageCategories?.nodes.orEmpty()
                val category = newCategories.firstOrNull { it.id == viewModelStateFlow.value.categoryId }
                viewModelStateFlow.update { viewModelState ->
                    val newContentType = run type@{
                        if (viewModelState.categories.isNotEmpty()) {
                            return@type viewModelState.contentType
                        }
                        if (category != null) {
                            ViewModelState.ContentType.Category(
                                categoryId = category.id,
                                name = category.name,
                            )
                        } else {
                            ViewModelState.ContentType.All
                        }
                    }
                    viewModelState.copy(
                        categories = newCategories,
                        contentType = newContentType,
                    )
                }
            }
        }
    }

    private fun createCategoryTypes(categories: List<RootHomeTabScreenQuery.Node>): List<RootHomeTabPeriodAndCategoryUiState.CategoryTypes> {
        return buildList {
            add(
                RootHomeTabPeriodAndCategoryUiState.CategoryTypes(
                    title = "すべて",
                    onClick = {
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                contentType = ViewModelState.ContentType.All,
                            )
                        }

                        viewModelScope.launch {
                            viewModelEventSender.send {
                                it.onClickAllFilter()
                            }
                        }
                    },
                ),
            )
            addAll(
                categories.map { category ->
                    RootHomeTabPeriodAndCategoryUiState.CategoryTypes(
                        title = category.name,
                        onClick = {
                            viewModelScope.launch {
                                viewModelEventSender.send {
                                    it.onClickCategoryFilter(category.id)
                                }
                            }
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    contentType = ViewModelState.ContentType.Category(
                                        categoryId = category.id,
                                        name = category.name,
                                    ),
                                )
                            }
                        },
                    )
                },
            )
        }
    }

    public interface Event {
        public fun onClickAllFilter()

        public fun onClickCategoryFilter(categoryId: MoneyUsageCategoryId)

        public fun updateSinceDate(
            year: Int,
            month: Int,
            period: Int,
        )
    }

    private data class ViewModelState(
        val categoryId: MoneyUsageCategoryId?,
        val contentType: ContentType = ContentType.Loading,
        val categories: List<RootHomeTabScreenQuery.Node> = listOf(),
        val displayPeriod: Period = run {
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            Period(
                sinceDate = YearMonth(currentDate.year, currentDate.monthNumber).addMonth(-5),
                monthCount = 6,
            )
        },
    ) {
        data class YearMonth(
            val year: Int,
            val month: Int,
        ) {
            fun addMonth(count: Int): YearMonth {
                val nextDate = LocalDate(year, month, 1)
                    .plus(count, DateTimeUnit.MONTH)

                return YearMonth(
                    year = nextDate.year,
                    month = nextDate.monthNumber,
                )
            }
        }

        data class Period(
            val sinceDate: YearMonth,
            val monthCount: Int,
        )

        sealed interface ContentType {
            data object Loading : ContentType

            data object All : ContentType

            data class Category(
                val categoryId: MoneyUsageCategoryId,
                val name: String,
            ) : ContentType
        }
    }
}
