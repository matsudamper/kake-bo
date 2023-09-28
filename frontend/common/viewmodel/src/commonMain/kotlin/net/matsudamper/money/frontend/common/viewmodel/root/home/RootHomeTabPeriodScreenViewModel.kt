package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
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
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenQuery

public class RootHomeTabPeriodScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
    private val categoryId: MoneyUsageCategoryId?,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val event = object : RootHomeTabPeriodUiState.Event {
        override fun onClickNextMonth() {
            viewModelStateFlow.update {
                val period = viewModelStateFlow.value.displayPeriod
                it.copy(
                    displayPeriod = period.copy(
                        sinceDate = period.sinceDate.addMonth(1),
                    ),
                )
            }
            updateUrl()
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
            updateUrl()
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
            updateUrl()
        }

        override fun onViewInitialized() {
            collectScreenInfo()
        }

        // TODO Remove
        override fun onClickRetry() {
        }
    }

    public val uiStateFlow: StateFlow<RootHomeTabPeriodUiState> = MutableStateFlow(
        RootHomeTabPeriodUiState(
            loadingState = RootHomeTabPeriodUiState.LoadingState.Loading,
            event = event,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val loadingState = run screenState@{
                    val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                        viewModelState.displayPeriod.sinceDate.addMonth(index)
                    }

                    RootHomeTabPeriodUiState.LoadingState.Loaded(
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

                uiStateFlow.value = RootHomeTabPeriodUiState(
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

    private fun collectScreenInfo() {
        coroutineScope.launch {
            api.screenFlow().collectLatest { response ->
                val newCategories = response.data?.user?.moneyUsageCategories?.nodes.orEmpty()
                val category = newCategories.firstOrNull { it.id == categoryId }
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

    private fun createCategoryTypes(
        categories: List<RootHomeTabScreenQuery.Node>,
    ): List<RootHomeTabPeriodUiState.CategoryTypes> {
        return buildList {
            add(
                RootHomeTabPeriodUiState.CategoryTypes(
                    title = "すべて",
                    onClick = {
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                contentType = ViewModelState.ContentType.All,
                            )
                        }

                        coroutineScope.launch {
                            viewModelEventSender.send {
                                it.onClickAllFilter()
                            }
                        }
                    },
                ),
            )
            addAll(
                categories.map { category ->
                    RootHomeTabPeriodUiState.CategoryTypes(
                        title = category.name,
                        onClick = {
                            coroutineScope.launch {
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

    private fun updateUrl() {
        coroutineScope.launch {
            viewModelEventSender.send {
                if (categoryId == null) {
                    it.onClickAllFilter()
                } else {
                    it.onClickCategoryFilter(categoryId = categoryId)
                }
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

    public interface Event {
        public fun onClickAllFilter()
        public fun onClickCategoryFilter(categoryId: MoneyUsageCategoryId)
    }

    private data class ViewModelState(
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
