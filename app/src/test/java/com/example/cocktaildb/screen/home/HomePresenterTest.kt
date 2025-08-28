package com.example.cocktaildb.screen.home

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.data.service.RecipeFirebaseService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class HomePresenterTest {

    private lateinit var closeable: AutoCloseable
    private lateinit var presenter: HomePresenter
    private lateinit var view: HomeViewFake

    private val dispatcher = StandardTestDispatcher()

    @Mock lateinit var cocktailRepo: CocktailRepository
    @Mock lateinit var authRepo: AuthRepository
    @Mock lateinit var historyService: HistoryFirebaseService
    @Mock lateinit var recipeService: RecipeFirebaseService

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)

        presenter = HomePresenter(
            cocktailRepository = cocktailRepo,
            authRepository = authRepo,
            historyFirebaseService = historyService,
            recipeFirebaseService = recipeService,
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher
        )
        view = HomeViewFake()
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    @Test
    fun loadCocktails_success_showsCocktails() = runTest(dispatcher) {
        val list = listOf(
            Cocktail(
                idDrink = "1",
                strDrink = "Mojito",
                strCategory = "Cocktail",
                strAlcoholic = "Alcoholic"
            )
        )
        `when`(cocktailRepo.fetchCocktailsFromApi()).thenReturn(list)

        presenter.loadCocktails()
        advanceUntilIdle()

        assert(view.lastShownCocktails == list)
    }

    @Test
    fun onCocktailClicked_navigatesToDetail() = runTest(dispatcher) {
        val c = Cocktail(
            idDrink = "2",
            strDrink = "Negroni",
            strCategory = "Cocktail",
            strAlcoholic = "Alcoholic"
        )

        presenter.onCocktailClicked(c)
        advanceUntilIdle()

        assert(view.lastNavigatedCocktail == c)
    }

    private class HomeViewFake : HomeContract.View {
        var lastShownCocktails: List<Cocktail>? = null
        var lastShownSharedCocktails: List<Cocktail>? = null
        var lastNavigatedCocktail: Cocktail? = null
        var lastMessage: String? = null

        override fun showCocktails(cocktails: List<Cocktail>) {
            lastShownCocktails = cocktails
        }

        override fun showSharedCocktails(cocktails: List<Cocktail>) {
            lastShownSharedCocktails = cocktails
        }

        override fun navigateToCocktailDetail(cocktail: Cocktail) {
            lastNavigatedCocktail = cocktail
        }

        override fun showMessage(message: String) {
            lastMessage = message
        }
    }
}
