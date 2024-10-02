package com.fetch.exercise.ui.list

import com.fetch.exercise.data.FakeFetchAPI
import com.fetch.exercise.data.FetchItemResponse
import com.fetch.exercise.data.NetworkResult
import com.fetch.exercise.utils.ObserveIsNetworkAvailable
import com.slack.circuit.test.test
import io.kotest.matchers.collections.exist
import io.kotest.matchers.collections.shouldBeSorted
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.instanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

private val RESPONSE_GROUP_1_ITEM_1 = FetchItemResponse(id = 1, listId = 1, name = "Item 1")
private val RESPONSE_GROUP_1_ITEM_3 = FetchItemResponse(id = 3, listId = 1, name = "Item 3")
private val RESPONSE_GROUP_2_ITEM_2 = FetchItemResponse(id = 2, listId = 2, name = "Item 2")
private val RESPONSE_GROUP_2_ITEM_4 = FetchItemResponse(id = 4, listId = 2, name = "Item 4")
private val RESPONSE_GROUP_2_ITEM_NULL = FetchItemResponse(id = 4, listId = 2, name = null)
private val RESPONSE_GROUP_2_ITEM_BLANK = FetchItemResponse(id = 4, listId = 2, name = " ")
private val RESPONSES = listOf(
    RESPONSE_GROUP_2_ITEM_BLANK,
    RESPONSE_GROUP_1_ITEM_1,
    RESPONSE_GROUP_1_ITEM_3,
    RESPONSE_GROUP_2_ITEM_4,
    RESPONSE_GROUP_2_ITEM_2,
    RESPONSE_GROUP_2_ITEM_NULL,
)

private val GROUP_1 = ListState.Group(
    id = 1,
    name = "1",
    items = persistentListOf(
        ListState.Item(id = 1, name = "Item 1"),
        ListState.Item(id = 3, name = "Item 3"),
    ),
)
private val GROUP_2 = ListState.Group(
    id = 2,
    name = "2",
    items = persistentListOf(
        ListState.Item(id = 2, name = "Item 2"),
        ListState.Item(id = 4, name = "Item 4"),
    ),
)
private val GROUPS = persistentListOf(GROUP_1, GROUP_2)

/** Arbitrary of any item that the server might return. */
private val Arb.Companion.responseItem: Arb<FetchItemResponse>
    get() = Arb.bind(
        Arb.int(1..100), // id
        Arb.int(1..100), // listId
        Arb.string(0..100).orNull(), // name
        ::FetchItemResponse,
    )

private val Arb.Companion.response: Arb<List<FetchItemResponse>>
    get() = Arb.list(Arb.responseItem, 0..100)
        .filter { response -> response.distinctBy { it.id } == response }

@OptIn(ExperimentalCoroutinesApi::class)
class ListPresenterTest {

    private val isNetworkAvailableFlow = MutableStateFlow(true)
    private val observeIsNetworkAvailable: ObserveIsNetworkAvailable = mockk {
        every { this@mockk.invoke() } returns isNetworkAvailableFlow
    }

    @Test
    fun `Loading state initially`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will succeed
        isNetworkAvailableFlow.emit(true)
        fetchAPI.setItemsResult(NetworkResult.Success(RESPONSES))

        // When: The presenter is presented
        presenter.test {
            val initialState = awaitItem()

            // Then: The initial state is loading
            initialState.loadState shouldBe instanceOf<ListState.LoadState.Loading>()
        }
    }

    @Test
    fun `List can be loaded successfully`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = spyk(FakeFetchAPI())
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will succeed
        isNetworkAvailableFlow.emit(true)
        fetchAPI.setItemsResult(NetworkResult.Success(RESPONSES))

        // When: The presenter is presented
        presenter.test {
            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            val state = expectMostRecentItem()

            // Then: The state is loaded with the fetched items
            state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
            (state.loadState as ListState.LoadState.Success).groups shouldBe GROUPS

            // And: The fetch API was called once
            coVerify(exactly = 1) { fetchAPI.getItems() }
        }
    }

    @Test
    fun `No network is available`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is not available
        isNetworkAvailableFlow.emit(false)
        fetchAPI.setItemsResult(NetworkResult.NoInternet)

        // When: The presenter is presented
        presenter.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()

            // Then: The state is loaded with the no network error
            state.loadState shouldBe instanceOf<ListState.LoadState.Error>()
        }
    }

    @Test
    fun `Network request times out`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will time out
        isNetworkAvailableFlow.emit(true)
        fetchAPI.setItemsResult(NetworkResult.Timeout)

        // When: The presenter is presented
        presenter.test {
            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            val state = expectMostRecentItem()

            // Then: The state is loaded with the timeout error
            state.loadState shouldBe instanceOf<ListState.LoadState.Error>()
        }
    }

    @Test
    fun `Server is down`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and server is down
        isNetworkAvailableFlow.emit(true)
        fetchAPI.setItemsResult(NetworkResult.ServerDown)

        // When: The presenter is presented
        presenter.test {
            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            val state = expectMostRecentItem()

            // Then: The state is loaded with the server down error
            state.loadState shouldBe instanceOf<ListState.LoadState.Error>()
        }
    }

    @Test
    fun `User retries after network is recovered`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = spyk(FakeFetchAPI())
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will fail
        isNetworkAvailableFlow.emit(true)
        fetchAPI.setItemsResult(NetworkResult.Timeout)

        presenter.test {
            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            var state = expectMostRecentItem()

            // When: Network recovers
            isNetworkAvailableFlow.emit(true)
            fetchAPI.setItemsResult(NetworkResult.Success(RESPONSES))
            clearMocks(fetchAPI)

            // And: The user tries again
            state.eventSink(ListUIEvent.TryAgain)

            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            state = expectMostRecentItem()

            // Then: The state is loaded with the fetched items
            state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
            (state.loadState as ListState.LoadState.Success).groups shouldBe GROUPS

            // And: The fetch API was called once since the retry
            coVerify(exactly = 1) { fetchAPI.getItems() }
        }
    }

    @Test
    fun `User retries while network is down`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is not available
        isNetworkAvailableFlow.emit(false)
        fetchAPI.setItemsResult(NetworkResult.NoInternet)

        presenter.test {
            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            var state = expectMostRecentItem()

            // When: Network is still down
            isNetworkAvailableFlow.emit(false)

            // And: The user tries again
            state.eventSink(ListUIEvent.TryAgain)

            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            state = expectMostRecentItem()

            // Then: The state is still loaded with the no network error
            state.loadState shouldBe instanceOf<ListState.LoadState.Error>()
        }
    }

    @Test
    fun `Loading while app is retrying`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will fail
        isNetworkAvailableFlow.emit(true)
        fetchAPI.setItemsResult(NetworkResult.Timeout)

        presenter.test {
            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            var state = expectMostRecentItem()

            // When: Network recovers
            isNetworkAvailableFlow.emit(true)
            fetchAPI.setItemsResult(NetworkResult.Success(RESPONSES))

            // And: The user tries again
            state.eventSink(ListUIEvent.TryAgain)
            runCurrent()

            // Then: State changes to loading
            state = expectMostRecentItem()
            state.loadState shouldBe instanceOf<ListState.LoadState.Loading>()
        }
    }

    @Test
    fun `Network error retried automatically after network is recovered`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = spyk(FakeFetchAPI())
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is not available and network request will fail
        isNetworkAvailableFlow.emit(false)
        fetchAPI.setItemsResult(NetworkResult.NoInternet)

        presenter.test {
            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()

            // When: Network recovers
            clearMocks(fetchAPI)
            fetchAPI.setItemsResult(NetworkResult.Success(RESPONSES))
            isNetworkAvailableFlow.emit(true)

            // And: Enough time has passed for the network request to complete
            advanceUntilIdle()
            val state = expectMostRecentItem()

            // Then: The state is loaded with the fetched items
            state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
            (state.loadState as ListState.LoadState.Success).groups shouldBe GROUPS

            // And: The fetch API was called once since the retry
            coVerify(exactly = 1) { fetchAPI.getItems() }
        }
    }

    @Test
    fun `Network request auto-retried if flaky network connection`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and initial network requests will timeout
        isNetworkAvailableFlow.emit(true)
        fetchAPI.setItemsResult(NetworkResult.Timeout)

        presenter.test {
            // But: Next network request will succeed
            advanceTimeBy(2.seconds)
            fetchAPI.setItemsResult(NetworkResult.Success(RESPONSES))

            // When: some time passes
            advanceUntilIdle()
            val state = expectMostRecentItem()

            // Then: The state is loaded with the fetched items
            state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
            (state.loadState as ListState.LoadState.Success).groups shouldBe GROUPS
        }
    }

    @Test
    fun `Blank items are removed from the list`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will succeed
        isNetworkAvailableFlow.emit(true)
        checkAll(Arb.response) { response ->
            fetchAPI.setItemsResult(NetworkResult.Success(response))

            // When: The presenter is presented
            presenter.test {
                // And: Enough time has passed for the network request to complete
                advanceUntilIdle()
                val state = expectMostRecentItem()

                // Then: The state is loaded with the fetched items
                state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
                val groups = (state.loadState as ListState.LoadState.Success).groups
                val allItems = groups.flatMap { it.items }
                allItems shouldNot exist { it.name.isBlank() }
            }
        }
    }

    @Test
    fun `Null items are removed from the list`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will succeed
        isNetworkAvailableFlow.emit(true)
        checkAll(Arb.response) { response ->
            fetchAPI.setItemsResult(NetworkResult.Success(response))

            // When: The presenter is presented
            presenter.test {
                // And: Enough time has passed for the network request to complete
                advanceUntilIdle()
                val state = expectMostRecentItem()

                // Then: The state is loaded with the fetched items
                state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
                val groups = (state.loadState as ListState.LoadState.Success).groups
                val allItems = groups.flatMap { it.items }
                allItems shouldNot exist { it.name == null }
            }
        }
    }

    @Test
    fun `Items are grouped by listId`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will succeed
        isNetworkAvailableFlow.emit(true)
        checkAll(Arb.response) { response ->
            fetchAPI.setItemsResult(NetworkResult.Success(response))

            // When: The presenter is presented
            presenter.test {
                // And: Enough time has passed for the network request to complete
                advanceUntilIdle()
                val state = expectMostRecentItem()

                // Then: Items are grouped by listId
                state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
                val groups = (state.loadState as ListState.LoadState.Success).groups

                val responseItemIdToGroupIdMap = response.associate { it.id to it.listId }
                groups.forEach { group ->
                    group.items.forEach { item ->
                        group.id shouldBe responseItemIdToGroupIdMap[item.id]
                    }
                }
            }
        }
    }

    @Test
    fun `Groups are sorted by listId`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will succeed
        isNetworkAvailableFlow.emit(true)
        checkAll(Arb.response) { response ->
            fetchAPI.setItemsResult(NetworkResult.Success(response))

            // When: The presenter is presented
            presenter.test {
                // And: Enough time has passed for the network request to complete
                advanceUntilIdle()
                val state = expectMostRecentItem()

                // Then: Groups are sorted by listId
                state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
                val groups = (state.loadState as ListState.LoadState.Success).groups
                val allListIds = groups.map { it.id }
                allListIds.shouldBeSorted()
                allListIds.shouldNotContainDuplicates()
            }
        }
    }

    @Test
    fun `Items within same group are sorted by name`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will succeed
        isNetworkAvailableFlow.emit(true)
        checkAll(Arb.response) { response ->
            fetchAPI.setItemsResult(NetworkResult.Success(response))

            // When: The presenter is presented
            presenter.test {
                // And: Enough time has passed for the network request to complete
                advanceUntilIdle()
                val state = expectMostRecentItem()

                // Then: Items within same group are sorted by name
                state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
                val groups = (state.loadState as ListState.LoadState.Success).groups
                groups.forEach { group ->
                    val allItemNames = group.items.map { it.name }
                    allItemNames.shouldBeSorted()
                }
            }
        }
    }

    @Test
    fun `Valid items are not filtered out`() = runTest {
        // Given: A ListPresenter
        val fetchAPI = FakeFetchAPI()
        val presenter = ListPresenter(fetchAPI, observeIsNetworkAvailable)

        // And: Network is available and network request will succeed
        isNetworkAvailableFlow.emit(true)
        val validResponseArb = Arb.response.filter { response ->
            response.none { it.name.isNullOrBlank() }
        }
        checkAll(validResponseArb) { response ->
            fetchAPI.setItemsResult(NetworkResult.Success(response))

            // When: The presenter is presented
            presenter.test {
                // And: Enough time has passed for the network request to complete
                advanceUntilIdle()
                val state = expectMostRecentItem()

                // Then: The state is loaded with the fetched items
                state.loadState shouldBe instanceOf<ListState.LoadState.Success>()
                val groups = (state.loadState as ListState.LoadState.Success).groups
                val allItems = groups.flatMap { it.items }
                allItems shouldHaveSize response.size
                allItems.shouldContainAll(response.map { ListState.Item(it.id, it.name!!) })
            }
        }
    }
}
