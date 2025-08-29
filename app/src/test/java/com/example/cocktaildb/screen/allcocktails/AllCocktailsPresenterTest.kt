package com.example.cocktaildb.screen.allcocktails

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AllCocktailsPresenterTest {

    private lateinit var presenter: AllCocktailsPresenter
    private lateinit var closeable: AutoCloseable

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Mock lateinit var cocktailRepo: CocktailRepository
    @Mock lateinit var authRepo: AuthRepository
    @Mock lateinit var historyService: HistoryFirebaseService
    @Mock lateinit var view: AllCocktailsContract.View

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(mainDispatcher)

        presenter = AllCocktailsPresenter(
            cocktailRepository = cocktailRepo,
            authRepository = authRepo,
            historyFirebaseService = historyService,
            mainDispatcher = mainDispatcher,
            ioDispatcher = mainDispatcher
        )
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        closeable.close()
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllCocktails_loadsFromApi_whenNoCache() = runTest {
        val items = (1..25).map {
            Cocktail(
                idDrink = it.toString(),
                strDrink = "Drink$it",
                strCategory = if (it % 2 == 0) "CatA" else "CatB",
                strAlcoholic = if (it % 2 == 0) "Alcoholic" else "Non alcoholic"
            )
        }
        Mockito.`when`(cocktailRepo.getAllCocktails()).thenReturn(items)

        presenter.clearCache()
        presenter.loadAllCocktails()

        verify(view).showLoadingState()
        verify(view).showCocktails(items.take(10))
        verify(view).hideLoadingState()
        verify(view).updatePagination(1, 3, true, false)
        verify(view).showPagination(true)
    }

    @Test
    fun pagination_nextAndPrevious_changesPages() = runTest {
        val items = (1..21).map {
            Cocktail(
                idDrink = it.toString(),
                strDrink = "Drink$it",
                strCategory = "Cat",
                strAlcoholic = "Alcoholic"
            )
        }
        Mockito.`when`(cocktailRepo.getAllCocktails()).thenReturn(items)

        presenter.clearCache()
        presenter.loadAllCocktails()

        presenter.nextPage()
        verify(view, Mockito.atLeastOnce()).showCocktails(items.slice(20 until 21))
        presenter.nextPage()
        verify(view, Mockito.atLeastOnce()).showCocktails(items.slice(20 until 21))
        presenter.previousPage()
        verify(view, Mockito.atLeastOnce()).showCocktails(items.slice(10 until 20))
    }

    @Test
    fun search_filtersResults_andResetsToFirstPage() = runTest {
        val items = listOf(
            Cocktail(idDrink = "1", strDrink = "Mojito", strCategory = "Classic", strAlcoholic = "Alcoholic"),
            Cocktail(idDrink = "2", strDrink = "Virgin Mojito", strCategory = "Classic", strAlcoholic = "Non alcoholic"),
            Cocktail(idDrink = "3", strDrink = "Negroni", strCategory = "Classic", strAlcoholic = "Alcoholic")
        )
        Mockito.`when`(cocktailRepo.getAllCocktails()).thenReturn(items)

        presenter.clearCache()
        presenter.loadAllCocktails()
        presenter.searchCocktails("mojito")

        verify(view).showCocktails(items.filter { it.strDrink.contains("mojito", true) })
    }

    @Test
    fun filters_byCategory_andAlcoholic() = runTest {
        val items = listOf(
            Cocktail(idDrink = "1", strDrink = "A", strCategory = "CatA", strAlcoholic = "Alcoholic"),
            Cocktail(idDrink = "2", strDrink = "B", strCategory = "CatB", strAlcoholic = "Non alcoholic")
        )
        Mockito.`when`(cocktailRepo.getAllCocktails()).thenReturn(items)

        presenter.clearCache()
        presenter.loadAllCocktails()

        presenter.filterByCategory("CatA")
        verify(view).showCocktails(listOf(items[0]))

        presenter.filterByAlcoholic("Non alcoholic")
        verify(view, Mockito.atLeastOnce()).showCocktails(listOf(items[1]))
    }
}
