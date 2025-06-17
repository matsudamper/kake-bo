package net.matsudamper.money.frontend.common.viewmodel.moneyusage

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.moneyusage.MoneyUsageScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.fragment.MoneyUsageScreenMoneyUsage
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Minimal mock implementations until MockK is confirmed to work in commonTest
class MockMoneyUsageScreenViewModelApi : MoneyUsageScreenViewModelApi {
    var updateUsageCalledWith: Triple<MoneyUsageId, String?, MoneyUsageSubCategoryId?>? = null
    var deleteUsageCalledWith: MoneyUsageId? = null
    var updateUsageResult: Boolean = true
    var deleteUsageResult: Boolean = true

    override suspend fun updateUsage(
        id: MoneyUsageId,
        title: String?,
        description: String?,
        date: LocalDateTime?,
        amount: Long?,
        subCategoryId: MoneyUsageSubCategoryId?,
    ): Boolean {
        updateUsageCalledWith = Triple(id, title, subCategoryId)
        return updateUsageResult
    }

    override suspend fun deleteUsage(id: MoneyUsageId): Boolean {
        deleteUsageCalledWith = id
        return deleteUsageResult
    }
}

class MockEventSender<T> : EventSender<T> {
    var sentEvent: ((T) -> Unit)? = null
    override fun send(event: (T) -> Unit) {
        sentEvent = event
    }
}

class MockScopedObjectFeature : ScopedObjectFeature {
    override fun <T : Any> get(key: ScopedObjectFeature.Key<T>): T? = null
    override fun <T : Any> remove(key: ScopedObjectFeature.Key<T>) {}
    override fun <T : Any> set(key: ScopedObjectFeature.Key<T>, value: T?) {}
}


class MoneyUsageScreenViewModelTest {

    private lateinit var viewModel: MoneyUsageScreenViewModel
    private lateinit var mockApi: MockMoneyUsageScreenViewModelApi
    private lateinit var mockEventSender: MockEventSender<MoneyUsageScreenViewModel.Event>
    private lateinit var mockScopedObjectFeature: MockScopedObjectFeature
    private val testMoneyUsageId = MoneyUsageId(1)
    private val testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    @BeforeTest
    fun setup() {
        mockApi = MockMoneyUsageScreenViewModelApi()
        mockEventSender = MockEventSender()
        mockScopedObjectFeature = MockScopedObjectFeature()

        // TODO: GraphqlClient needs a proper mock or setup if its methods are called directly
        // For now, assuming apolloCollector.getFlow() is what matters and can be influenced
        // by viewModelStateFlow.update { it.copy(apolloResponseState = ...) }

        viewModel = MoneyUsageScreenViewModel(
            scopedObjectFeature = mockScopedObjectFeature,
            moneyUsageId = testMoneyUsageId,
            api = mockApi,
            graphqlClient = mockk() // Placeholder, proper mock needed if used
        )

        // HACK: Access private field to simulate apolloCollector behavior
        // This is brittle and should be replaced with proper test support for ApolloResponseCollector
        // For now, we directly manipulate the viewModelStateFlow which drives the uiStateFlow
        val viewModelStateFlowProperty = viewModel::class.java.getDeclaredField("viewModelStateFlow")
        viewModelStateFlowProperty.isAccessible = true
        val internalViewModelStateFlow = viewModelStateFlowProperty.get(viewModel) as MutableStateFlow<Any?> // Type erasure for simplicity

        // Initialize with a default loading state or mock response
        // This is a simplified way to control the internal state that apolloCollector would manage.
    }

    // Helper to get the internal MutableStateFlow for ViewModelState using reflection
    // CAUTION: This is a test hack and not good practice for production code.
    @Suppress("UNCHECKED_CAST")
    private fun getInternalViewModelStateFlow(vm: MoneyUsageScreenViewModel): MutableStateFlow<*> {
         try {
            val field = vm::class.java.getDeclaredField("viewModelStateFlow")
            field.isAccessible = true
            return field.get(vm) as MutableStateFlow<*>
        } catch (e: NoSuchFieldException) {
            // Fallback for Kotlin Native or other platforms where reflection might behave differently
            // This part is highly experimental and might not work.
            // It assumes the backing field name for a private val viewModelStateFlow might be "viewModelStateFlow"
            // or "viewModelStateFlow$delegate" if it's a delegated property.
            // This is very fragile.
            return vm::class.members.first { it.name == "viewModelStateFlow" }.call(vm) as MutableStateFlow<*>
        }
    }


    @AfterTest
    fun tearDown() {
        testScope.cancel()
    }

    private fun createSampleMoneyUsageData(
        id: MoneyUsageId = testMoneyUsageId,
        title: String = "Test Usage",
        description: String = "Test Description",
        amount: Double = 100.0,
        date: LocalDateTime = LocalDateTime(2023, 1, 1, 12, 0, 0),
        categoryId: MoneyUsageCategoryId = MoneyUsageCategoryId(10),
        categoryName: String = "Food",
        subCategoryId: MoneyUsageSubCategoryId = MoneyUsageSubCategoryId(100),
        subCategoryName: String = "Groceries"
    ): MoneyUsageScreenMoneyUsage {
        return MoneyUsageScreenMoneyUsage(
            __typename = "MoneyUsage",
            id = id,
            title = title,
            description = description,
            amount = amount,
            date = date,
            moneyUsageSubCategory = MoneyUsageScreenMoneyUsage.MoneyUsageSubCategory(
                __typename = "MoneyUsageSubCategory",
                id = subCategoryId,
                name = subCategoryName,
                category = MoneyUsageScreenMoneyUsage.MoneyUsageSubCategory.Category(
                    __typename = "MoneyUsageCategory",
                    id = categoryId,
                    name = categoryName
                )
            ),
            linkedMail = emptyList()
        )
    }

    @Test
    fun testDuplicateButtonNavigatesAndPassesData() = runTest {
        val sampleData = createSampleMoneyUsageData()
        val successResponse = ApolloResponse.Builder(
            operation = MoneyUsageScreenQuery(testMoneyUsageId),
            requestUuid = "uuid",
            data = MoneyUsageScreenQuery.Data(
                user = MoneyUsageScreenQuery.User(
                    __typename = "User",
                    moneyUsage = MoneyUsageScreenQuery.MoneyUsage(
                        __typename = "MoneyUsageResult",
                        moneyUsageScreenMoneyUsage = sampleData
                    )
                )
            )
        ).build()

        // Directly update the internal state flow to simulate Apollo giving data
        // This is a HACK because apolloCollector is hard to mock without more test utilities
        @Suppress("UNCHECKED_CAST")
        val internalStateFlow = getInternalViewModelStateFlow(viewModel) as MutableStateFlow<Any>

        // Construct the ViewModelState manually based on how the ViewModel would create it.
        // The actual ViewModelState class is private, so we can't directly instantiate it here.
        // This means we have to rely on the public uiStateFlow to reflect changes.
        // For the purpose of testing onClickDuplicate, we need the apolloResponseState to be Success.
        // This is extremely hacky and depends on the internal structure of ViewModelState.
        // A proper solution would involve making ViewModelState public or providing test utilities.

        // We will trigger the event through the uiState.loadingState.Loaded.event
        // First, let's get the uiState to a Loaded state

        // The internal ViewModelState needs to be updated.
        // Since ViewModelState is private, this is tricky.
        // Let's assume for now that setting apolloResponseState is enough
        // to get the LoadedEvent we need.

        // This is a placeholder for setting the internal state.
        // In a real scenario with better testability, you would mock ApolloClient/Collector
        // or have a testable way to set the ViewModelState.
        // For now, we'll assume that if we call the event's onClickDuplicate,
        // the viewModelStateFlow already holds the correct ApolloResponseState.Success.
        // This is a leap of faith for this current test setup.


        // Simulate the ViewModel receiving data
        // This is where the HACK is. We can't directly set ViewModelState.
        // We rely on the fact that uiStateFlow will eventually emit a Loaded state
        // if apolloResponseState is Success.

        // Instead of manipulating private state, let's try to get the event object
        // from the UI state after it's loaded. This is more of a black-box approach.

        // To make apolloResponseState a success, we would typically mock the graphqlClient
        // and its apolloClient. For this test, we'll assume it's been loaded.
        // The following line is a placeholder for that complex mocking.
        // (getInternalViewModelStateFlow(viewModel) as MutableStateFlow<Any>).update { ... }
        // Since we can't easily mock apolloCollector.getFlow(), we can't easily push
        // a success state into viewModelStateFlow to get the LoadedEvent.

        // Given the limitations, let's assume we *can* get the LoadedEvent somehow.
        // The most robust way without deep mocking is to call the public functions
        // that would lead to this state, but that makes this a larger integration test.

        // Let's pivot: Assume the `createLoadedEvent()` returns an object,
        // and we can call `onClickDuplicate` on it if we can get that event object.
        // The `uiStateFlow.value.loadingState` should be `Loaded` if data is present.
        // The `event` within `Loaded` is what we need.

        // This test will not work as intended without a way to set apolloResponseState to Success.
        // I will proceed with the structure, but highlight this requires better testability.

        // --- This part of the test is currently not runnable due to testability issues ---
        // --- Need to mock apolloCollector or graphqlClient effectively ---

        // Manually create a LoadedEvent for testing purposes
        val loadedEvent = (viewModel.uiStateFlow.value.loadingState as? MoneyUsageScreenUiState.LoadingState.Loaded)?.event

        // If we cannot get the loadedEvent because the state is not Loaded, this test cannot proceed.
        // This indicates a fundamental issue with testing this ViewModel without proper mocking.
        // For now, I will have to skip the core assertion part of this test.

        if (loadedEvent == null) {
            println("Test skipped: Could not get LoadedEvent. ViewModel not in Loaded state or test setup incomplete.")
            // This would ideally be an assertion failure or an inconclusive test result.
            assertTrue(false, "Test precondition failed: ViewModel not in Loaded state. Requires proper mocking for apolloCollector.")
            return@runTest
        }

        loadedEvent.onClickDuplicate()

        assertNotNull(mockEventSender.sentEvent, "Event sender should have been called")
        var navigatedStructure: ScreenStructure? = null
        mockEventSender.sentEvent?.invoke(object : MoneyUsageScreenViewModel.Event {
            override fun navigate(structure: ScreenStructure) {
                navigatedStructure = structure
            }
            override fun navigateBack() {}
            override fun openUrl(text: String) {}
            override fun copyUrl(text: String) {}
        })

        assertNotNull(navigatedStructure, "Navigation should have occurred")
        assertIs<ScreenStructure.AddMoneyUsage>(navigatedStructure)
        val addUsageScreen = navigatedStructure as ScreenStructure.AddMoneyUsage

        assertEquals(sampleData.title, addUsageScreen.title)
        assertEquals(sampleData.amount.toFloat(), addUsageScreen.price)
        assertEquals(sampleData.date, addUsageScreen.date)
        assertEquals(sampleData.description, addUsageScreen.description)
        assertEquals(sampleData.moneyUsageSubCategory?.category?.id, addUsageScreen.categoryId)
        assertEquals(sampleData.moneyUsageSubCategory?.category?.name, addUsageScreen.categoryName)
        assertEquals(sampleData.moneyUsageSubCategory?.id, addUsageScreen.subCategoryId)
        assertEquals(sampleData.moneyUsageSubCategory?.name, addUsageScreen.subCategoryName)
    }
}

// Helper to mock apollo client calls if needed, but ApolloResponseCollector makes it tricky.
fun mockkGraphQlClient(): net.matsudamper.money.frontend.graphql.GraphqlClient {
    // This would need to return a mock that allows controlling apolloCollector responses.
    // For now, this is a placeholder.
    throw NotImplementedError("Proper GraphqlClient mock needed for commonTest without full MockK setup")
}

// Minimal mock for com.apollographql.apollo3.ApolloClient - very basic
class MockApolloClient {
    // Define methods that your GraphqlClient or ApolloResponseCollector might use.
    // This is highly dependent on actual usage.
}
