package net.matsudamper.money.frontend.common.ui.screenshot

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.StickyHeaderState
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChartItem
import net.matsudamper.money.frontend.common.ui.screen.addmoneyusage.AddMoneyUsageScreen
import net.matsudamper.money.frontend.common.ui.screen.addmoneyusage.AddMoneyUsageScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.SortSectionOrder
import net.matsudamper.money.frontend.common.ui.screen.root.home.SortSectionType
import net.matsudamper.money.frontend.common.ui.screen.root.home.monthly.RootHomeMonthlyScreen
import net.matsudamper.money.frontend.common.ui.screen.root.home.monthly.RootHomeMonthlyScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.settings.RootSettingScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingRootScreen
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreen
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreenUiState
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
    )

    @Test
    fun loginScreen() {
        paparazzi.snapshot {
            AppRoot(isDarkTheme = false) {
                LoginScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = LoginScreenUiState(
                        userName = TextFieldValue(""),
                        password = TextFieldValue(""),
                        serverHost = null,
                        listener = object : LoginScreenUiState.Listener {
                            override fun onClickLogin() {}
                            override fun onClickNavigateAdmin() {}
                            override fun onClickSecurityKeyLogin() {}
                            override fun onClickDeviceKeyLogin() {}
                            override fun onUserIdChanged(text: String) {}
                            override fun onPasswordChanged(text: String) {}
                            override fun onClickChangeHost() {}
                            override fun onCustomHostTextChanged(text: String) {}
                            override fun onConfirmCustomHost() {}
                            override fun onDismissCustomHostDialog() {}
                        },
                    ),
                    windowInsets = PaddingValues(),
                )
            }
        }
    }

    @Test
    fun homeMonthlyScreen() {
        val noOpItemEvent = object : RootHomeMonthlyScreenUiState.ItemEvent {
            override fun onClick() {}
        }
        paparazzi.snapshot {
            AppRoot(isDarkTheme = false) {
                RootHomeMonthlyScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = RootHomeMonthlyScreenUiState(
                        loadingState = RootHomeMonthlyScreenUiState.LoadingState.Loaded(
                            yearMonth = "2026年2月",
                            totalAmount = "¥128,450",
                            items = listOf(
                                RootHomeMonthlyScreenUiState.Item(
                                    title = "Amazon.co.jp",
                                    amount = "¥3,280",
                                    date = "2026/02/25",
                                    category = "ショッピング",
                                    event = noOpItemEvent,
                                ),
                                RootHomeMonthlyScreenUiState.Item(
                                    title = "スーパーマーケット",
                                    amount = "¥5,430",
                                    date = "2026/02/24",
                                    category = "食費",
                                    event = noOpItemEvent,
                                ),
                                RootHomeMonthlyScreenUiState.Item(
                                    title = "電気料金",
                                    amount = "¥8,200",
                                    date = "2026/02/20",
                                    category = "光熱費",
                                    event = noOpItemEvent,
                                ),
                            ).toImmutableList(),
                            pieChartItems = listOf(
                                PieChartItem(color = Color(0xFF558B2F), title = "食費", value = 45000),
                                PieChartItem(color = Color(0xFF1565C0), title = "光熱費", value = 25000),
                                PieChartItem(color = Color(0xFFE65100), title = "ショッピング", value = 32000),
                            ).toImmutableList(),
                            hasMoreItem = false,
                            event = object : RootHomeMonthlyScreenUiState.LoadedEvent {
                                override fun loadMore() {}
                            },
                        ),
                        event = object : RootHomeMonthlyScreenUiState.Event {
                            override suspend fun onViewInitialized() {}
                            override fun onSortTypeChanged(sortType: SortSectionType) {}
                            override fun onSortOrderChanged(order: SortSectionOrder) {}
                        },
                        currentSortType = SortSectionType.Date,
                        sortOrder = SortSectionOrder.Descending,
                    ),
                    windowInsets = PaddingValues(),
                )
            }
        }
    }

    @Test
    fun addMoneyUsageScreen() {
        paparazzi.snapshot {
            AppRoot(isDarkTheme = false) {
                AddMoneyUsageScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = AddMoneyUsageScreenUiState(
                        calendarDialog = null,
                        timePickerDialog = null,
                        fullScreenTextInputDialog = null,
                        categorySelectDialog = null,
                        date = "2026/02/26",
                        time = "12:30",
                        title = "スーパーマーケット",
                        description = "食料品の購入",
                        category = "食費",
                        amount = "¥3,500",
                        images = ImmutableList(listOf()),
                        addButtonEnabled = true,
                        event = object : AddMoneyUsageScreenUiState.Event {
                            override fun onClickAdd() {}
                            override fun selectedCalendar(date: LocalDate) {}
                            override fun dismissCalendar() {}
                            override fun selectedTime(time: LocalTime) {}
                            override fun dismissTimePicker() {}
                            override fun onClickDateChange() {}
                            override fun onClickTimeChange() {}
                            override fun onClickTitleChange() {}
                            override fun onClickDescriptionChange() {}
                            override fun onClickCategoryChange() {}
                            override fun onClickAmountChange() {}
                            override fun onClickUploadImage() {}
                        },
                        numberInputDialog = null,
                    ),
                    windowInsets = PaddingValues(),
                )
            }
        }
    }

    @Test
    fun settingScreen() {
        paparazzi.snapshot {
            AppRoot(isDarkTheme = false) {
                SettingRootScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = RootSettingScreenUiState(
                        kotlinVersion = "2.3.10",
                        kakeboScaffoldListener = object : KakeboScaffoldListener {
                            override fun onClickTitle() {}
                        },
                        event = object : RootSettingScreenUiState.Event {
                            override fun onResume() {}
                            override fun onClickImapButton() {}
                            override fun onClickCategoryButton() {}
                            override fun onClickMailFilter() {}
                            override fun onClickGitHub() {}
                            override fun onClickLoginSetting() {}
                            override fun onClickApiSetting() {}
                            override fun onClickTextFieldTest() {}
                        },
                    ),
                    windowInsets = PaddingValues(),
                )
            }
        }
    }

    @Test
    fun calendarScreen() {
        val noOpDayCellEvent = object : RootUsageCalendarScreenUiState.DayCellEvent {
            override fun onClick() {}
        }
        val noOpCalendarDayEvent = object : RootUsageCalendarScreenUiState.CalendarDayEvent {
            override fun onClick() {}
        }
        val dayOfWeeks = listOf(
            kotlinx.datetime.DayOfWeek.SUNDAY to "日",
            kotlinx.datetime.DayOfWeek.MONDAY to "月",
            kotlinx.datetime.DayOfWeek.TUESDAY to "火",
            kotlinx.datetime.DayOfWeek.WEDNESDAY to "水",
            kotlinx.datetime.DayOfWeek.THURSDAY to "木",
            kotlinx.datetime.DayOfWeek.FRIDAY to "金",
            kotlinx.datetime.DayOfWeek.SATURDAY to "土",
        ).map { (dayOfWeek, text) ->
            RootUsageCalendarScreenUiState.CalendarCell.DayOfWeek(
                dayOfWeek = dayOfWeek,
                text = text,
            )
        }
        val days = (1..28).map { day ->
            RootUsageCalendarScreenUiState.CalendarCell.Day(
                text = day.toString(),
                isToday = day == 27,
                items = if (day == 25) {
                    listOf(
                        RootUsageCalendarScreenUiState.CalendarDayItem(
                            title = "Amazon",
                            color = Color(0xFF558B2F),
                            event = noOpCalendarDayEvent,
                        ),
                    ).toImmutableList()
                } else {
                    ImmutableList(listOf())
                },
                event = noOpDayCellEvent,
            )
        }
        val emptyCells = List(6) { RootUsageCalendarScreenUiState.CalendarCell.Empty }
        val calendarCells = (dayOfWeeks + emptyCells + days).toImmutableList()

        paparazzi.snapshot {
            AppRoot(isDarkTheme = false) {
                RootUsageCalendarScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = RootUsageCalendarScreenUiState(
                        event = object : RootUsageCalendarScreenUiState.Event {
                            override suspend fun onViewInitialized() {}
                            override fun refresh() {}
                        },
                        loadingState = RootUsageCalendarScreenUiState.LoadingState.Loaded(
                            calendarCells = calendarCells,
                            event = object : RootUsageCalendarScreenUiState.LoadedEvent {
                                override fun loadMore() {}
                            },
                        ),
                    ),
                    stickyHeaderState = StickyHeaderState(enterAlways = false),
                )
            }
        }
    }
}
