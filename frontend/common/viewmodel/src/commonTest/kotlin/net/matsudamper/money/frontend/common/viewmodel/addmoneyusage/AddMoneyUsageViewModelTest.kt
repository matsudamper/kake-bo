package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.NumberInputValue
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenGraphqlApi
import net.matsudamper.money.frontend.graphql.GetMailAttributesQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// Minimal ScopedObjectFeature mock
class MockScopedObjectFeature : ScopedObjectFeature {
    override fun <T : Any> get(key: ScopedObjectFeature.Key<T>): T? = null
    override fun <T : Any> remove(key: ScopedObjectFeature.Key<T>) {}
    override fun <T : Any> set(key: ScopedObjectFeature.Key<T>, value: T?) {}
}

// Minimal AddMoneyUsageScreenApi mock
class MockAddMoneyUsageScreenApi : AddMoneyUsageScreenApi {
    var addMoneyUsageCalled = false
    var getCalled = false

    override suspend fun addMoneyUsage(
        title: String,
        description: String,
        datetime: LocalDateTime,
        amount: Long,
        subCategoryId: MoneyUsageSubCategoryId?,
        importedMailId: net.matsudamper.money.element.ImportedMailId?,
    ): AddMoneyUsageScreenGraphqlApi.AddMoneyUsageResult? {
        addMoneyUsageCalled = true
        return null // Or a mock result
    }

    override suspend fun get(id: net.matsudamper.money.element.ImportedMailId): Result<GetMailAttributesQuery.Data> {
        getCalled = true
        // Return a dummy success or failure, depending on what needs to be tested
        return Result.success(
            GetMailAttributesQuery.Data(
                user = GetMailAttributesQuery.User(
                    __typename = "User",
                    importedMailAttributes = GetMailAttributesQuery.ImportedMailAttributes(
                        __typename = "ImportedMailAttributes",
                        mail = GetMailAttributesQuery.Mail(
                            __typename = "ImportedMail",
                            subject = "Test Mail Subject",
                            dateTime = Clock.System.now().toEpochMilliseconds().let {
                                LocalDateTime.parse("2023-01-01T10:00:00") // Consistent time
                            },
                            suggestUsages = emptyList(),
                            forwardedInfo = null
                        )
                    )
                )
            )
        )
    }
}

// Minimal GraphqlClient mock
class MockGraphqlClient : GraphqlClient(mockkApolloClient()) // Needs a mock ApolloClient
fun mockkApolloClient(): com.apollographql.apollo3.ApolloClient {
    // This is a placeholder. A real mock ApolloClient would be needed.
    // However, AddMoneyUsageViewModel uses it for CategorySelectDialogViewModel,
    // which we are not deeply testing here.
    // For the direct API calls (addMoneyUsage, get), we mock AddMoneyUsageScreenApi instead.
    return object : com.apollographql.apollo3.ApolloClient by error("MockApolloClient not fully implemented") {}
}


class AddMoneyUsageViewModelTest {

    private lateinit var viewModel: AddMoneyUsageViewModel
    private lateinit var mockApi: MockAddMoneyUsageScreenApi
    private lateinit var mockGraphqlClient: MockGraphqlClient
    private lateinit var mockScopedObjectFeature: MockScopedObjectFeature
    private val testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    @BeforeTest
    fun setup() {
        mockApi = MockAddMoneyUsageScreenApi()
        mockGraphqlClient = MockGraphqlClient() // Initialize with a mock ApolloClient if needed
        mockScopedObjectFeature = MockScopedObjectFeature()

        viewModel = AddMoneyUsageViewModel(
            scopedObjectFeature = mockScopedObjectFeature,
            graphqlApi = mockApi,
            graphqlClient = mockGraphqlClient
        )
    }

    @AfterTest
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun testPreFillDataFromScreenStructure() = runTest {
        val testDate = LocalDate(2023, 10, 26)
        val testTime = LocalTime(14, 30)
        val testDateTime = LocalDateTime(testDate, testTime)
        val testTitle = "Duplicated Item"
        val testPrice = 123.45f
        val testDescription = "This is a duplicated description."
        val testCategoryId = MoneyUsageCategoryId(1)
        val testCategoryName = "Test Category"
        val testSubCategoryId = MoneyUsageSubCategoryId(2)
        val testSubCategoryName = "Test SubCategory"

        val screenStructure = ScreenStructure.AddMoneyUsage(
            importedMailId = null,
            importedMailIndex = null,
            title = testTitle,
            price = testPrice,
            date = testDateTime,
            description = testDescription,
            categoryId = testCategoryId,
            categoryName = testCategoryName,
            subCategoryId = testSubCategoryId,
            subCategoryName = testSubCategoryName
        )

        viewModel.updateScreenStructure(screenStructure)

        val uiState = viewModel.uiStateFlow.first()

        assertEquals(testTitle, uiState.title)
        assertEquals(testPrice.toDouble().toString(), uiState.amount) // ViewModel formats amount as string
        assertEquals(testDescription, uiState.description)

        val expectedDateString = "${testDate.year}-${testDate.monthNumber}-${testDate.dayOfMonth} (${Formatter.dayOfWeekToJapanese(testDate.dayOfWeek)})"
        assertEquals(expectedDateString, uiState.date)

        val expectedCategoryString = "$testCategoryName / $testSubCategoryName"
        assertEquals(expectedCategoryString, uiState.category)

        // Verify internal ViewModelState (optional, if ViewModelState was public/testable)
        // val internalState = viewModel.viewModelStateFlow.first() // Assuming viewModelStateFlow is accessible
        // assertEquals(NumberInputValue.default(testPrice.toInt()), internalState.usageAmount)
        // assertEquals(testDate, internalState.usageDate)
        // assertEquals(testTime, internalState.usageTime)
        // assertNotNull(internalState.usageCategorySet)
        // assertEquals(testCategoryId, internalState.usageCategorySet?.categoryId)
        // assertEquals(testSubCategoryId, internalState.usageCategorySet?.subCategoryId)
    }

    @Test
    fun testPreFillDataWithMinimalScreenStructure() = runTest {
        // Test with only a few fields to ensure defaults are handled
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val defaultTime = LocalTime(0,0,0) // Default time in ViewModelState

        val screenStructure = ScreenStructure.AddMoneyUsage(
            title = "Minimal Data"
        )

        viewModel.updateScreenStructure(screenStructure)
        val uiState = viewModel.uiStateFlow.first()

        assertEquals("Minimal Data", uiState.title)
        assertEquals(NumberInputValue.default().value.toDouble().toString(), uiState.amount) // Default amount
        assertEquals("", uiState.description) // Default description

        val expectedDateString = "${today.year}-${today.monthNumber}-${today.dayOfMonth} (${Formatter.dayOfWeekToJapanese(today.dayOfWeek)})"
        assertEquals(expectedDateString, uiState.date) // Default date (today)
        assertEquals("未選択", uiState.category) // Default category
    }
}
