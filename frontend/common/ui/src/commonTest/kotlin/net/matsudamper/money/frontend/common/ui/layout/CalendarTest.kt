package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertDoesNotExist
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class CalendarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pickerAppearsOnClick() {
        val initialDate = LocalDate(2023, 10, 15)
        var selectedDate = initialDate

        composeTestRule.setContent {
            Calendar(
                selectedDate = selectedDate,
                changeSelectedDate = { newDate -> selectedDate = newDate },
            )
        }

        // Click on the year/month text, e.g., "2023年10月"
        composeTestRule.onNodeWithText("${initialDate.year}年${initialDate.monthNumber}月").performClick()

        // Assert that the YearMonthPicker is displayed (e.g., by checking for its title or Done button)
        composeTestRule.onNodeWithText("Select Year and Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun pickerUpdatesCalendarViewAndDismisses() {
        val initialDate = LocalDate(2023, 10, 15)
        var selectedDate = initialDate

        composeTestRule.setContent {
            Calendar(
                selectedDate = selectedDate,
                changeSelectedDate = { newDate -> selectedDate = newDate },
            )
        }

        // 1. Click on the year/month text to show the picker
        composeTestRule.onNodeWithText("${initialDate.year}年${initialDate.monthNumber}月").performClick()

        // Assert picker is shown
        composeTestRule.onNodeWithText("Select Year and Month").assertIsDisplayed()

        // 2. In the picker, select a new year and month
        val targetYear = 2024
        val targetMonth = 12
        val targetMonthString = targetMonth.toString() // Month is displayed as "1", "2", .. "12"

        // Select year (scroll if necessary, but for this test, direct click if visible)
        // Assuming years are laid out in a way that "2024" is clickable
        // If YearMonthPicker uses LazyRow, ensure the item is scrolled to if needed.
        // For simplicity, let's assume "2024" is visible or becomes visible.
        // Actual year items in picker are just text of the year.
        composeTestRule.onNodeWithText(targetYear.toString()).performClick()

        // Select month
        // Month items in picker are Buttons with text "1" through "12"
        composeTestRule.onNodeWithText(targetMonthString, substring = true, useUnmergedTree = true).performClick()


        // 3. Click the "Done" button in the picker
        composeTestRule.onNodeWithText("Done").performClick()

        // 4. Assert that the picker is no longer visible
        composeTestRule.onNodeWithText("Select Year and Month").assertDoesNotExist()
        composeTestRule.onNodeWithText("Done").assertDoesNotExist()

        // 5. Assert that the Calendar's displayed year/month text has updated
        // The visibleCalendarDate in Calendar composable should now reflect year=2024, month=12.
        // Since day is set to 1st of month on change:
        val expectedHeaderText = "${targetYear}年${targetMonth}月"
        composeTestRule.onNodeWithText(expectedHeaderText).assertIsDisplayed()

        // Also check that the selectedDate (if we were testing changeSelectedDate) is not yet changed,
        // as the picker only changes the internal visibleCalendarDate.
        // The changeSelectedDate callback is for day clicks.
        // However, our picker directly modifies visibleCalendarDate, which drives the header.
        // The `selectedDate` variable in this test scope is for the Calendar's `selectedDate` prop,
        // not its internal `visibleCalendarDate`.
        // The test for `changeSelectedDate` would be separate (clicking a day cell).
    }
}
