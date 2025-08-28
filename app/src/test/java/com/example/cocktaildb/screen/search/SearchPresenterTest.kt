package com.example.cocktaildb.screen.search

import android.os.Looper
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.data.service.SearchHistoryService
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml")
class SearchPresenterTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    private lateinit var presenter: SearchPresenter
    private lateinit var cocktailRepository: CocktailRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var historyFirebaseService: HistoryFirebaseService
    private lateinit var searchHistoryService: SearchHistoryService
    private lateinit var view: SearchContract.View

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        cocktailRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        historyFirebaseService = mockk(relaxed = true)
        searchHistoryService = mockk(relaxed = true)
        view = mockk(relaxed = true)

        presenter = SearchPresenter(
            cocktailRepository,
            authRepository,
            historyFirebaseService,
            searchHistoryService
        )
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        presenter.setView(null)
        Dispatchers.resetMain()
    }

    @Test
    fun `searchCocktails with empty query loads all cocktails and updates view`() {
        val cocktails = makeCocktails(15)
        every { cocktailRepository.getCocktails() } returns cocktails

        presenter.searchCocktails("")
        Thread.sleep(60)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify { view.showLoading() }
        verify { view.hideLoading() }
        verify { view.showCocktails(match { it.size == 10 }) }
        verify { view.updatePagination(any(), any(), any(), any()) }
        verify { view.showPagination(true) }
    }

    @Test
    fun `filterByCategory without current search uses repository filter and updates view`() {
        val filtered = makeCocktails(8, category = "Ordinary Drink")
        every { cocktailRepository.filterByCategory("Ordinary Drink") } returns filtered

        presenter.filterByCategory("Ordinary Drink")

        Thread.sleep(60)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify { view.showLoading() }
        verify { view.hideLoading() }
        verify { view.showCocktails(match { it.size == 8 }) }
    }

    @Test
    fun `pagination nextPage after initial search shows next items`() {
        val cocktails = makeCocktails(25)
        every { cocktailRepository.getCocktails() } returns cocktails

        presenter.searchCocktails("")
        Thread.sleep(60)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        presenter.nextPage()

        verify { view.showCocktails(match { it.size == 10 }) }
        verify { view.updatePagination(any(), any(), any(), any()) }
    }

    @Test
    fun `onSearchTextChanged shows suggestions after debounce`() = runTest(testDispatcher) {
        coEvery { searchHistoryService.getSuggestions("mo") } returns listOf("mojito", "mocha")
        presenter.onSearchTextChanged("mo")

        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()
        verify { view.showSearchSuggestions(listOf("mojito", "mocha")) }
    }

    @Test
    fun `onSearchSubmitted saves query and triggers search`() = runTest(testDispatcher) {
        val cocktails = makeCocktails(3)
        coEvery { searchHistoryService.addSearchQuery("margarita") } returns Unit
        every { cocktailRepository.searchCocktails("margarita") } returns cocktails

        presenter.onSearchSubmitted("margarita")

        testScheduler.runCurrent()
        Thread.sleep(60)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        coVerify { searchHistoryService.addSearchQuery("margarita") }
        verify { view.hideSearchSuggestions() }
        verify { view.showCocktails(match { it.size == 3 }) }
    }

    private fun makeCocktails(count: Int, category: String? = null, alcoholic: String? = null): List<Cocktail> {
        return (1..count).map { i ->
            Cocktail(
                idDrink = "id_$i",
                strDrink = "Cocktail $i",
                strCategory = category ?: if (i % 2 == 0) "Ordinary Drink" else "Cocktail",
                strAlcoholic = alcoholic ?: if (i % 2 == 0) "Alcoholic" else "Non alcoholic",
                strDrinkThumb = null
            )
        }
    }
}
