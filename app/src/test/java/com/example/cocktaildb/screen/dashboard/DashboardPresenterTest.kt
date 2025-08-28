package com.example.cocktaildb.screen.dashboard

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.TodayDrinkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardPresenterTest {

    private lateinit var presenter: DashboardPresenter
    private lateinit var closeable: AutoCloseable

    private val dispatcher = StandardTestDispatcher()

    @Mock lateinit var todayDrinkManager: TodayDrinkManager
    @Mock lateinit var view: DashboardContract.View

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(dispatcher)
        
        presenter = DashboardPresenter(
            todayDrinkManager = todayDrinkManager,
            mainDispatcher = dispatcher,
            ioDispatcher = dispatcher
        )
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    @Test
    fun loadDashboardData_showsDashboardData() {
        presenter.loadDashboardData()
        verify(view).showDashboardData()
    }

    @Test
    fun loadTodayDrink_success_showsTodayDrink() = runTest(dispatcher) {
        val todayDrink = Cocktail(
            idDrink = "1",
            strDrink = "Mojito",
            strCategory = "Cocktail",
            strAlcoholic = "Alcoholic"
        )
        `when`(todayDrinkManager.getTodayDrink()).thenReturn(todayDrink)

        presenter.loadTodayDrink()
        advanceUntilIdle()

        verify(view).showTodayDrink(todayDrink)
    }

    @Test
    fun loadTodayDrink_noDrink_showsMessageAndDashboard() = runTest(dispatcher) {
        `when`(todayDrinkManager.getTodayDrink()).thenReturn(null)

        presenter.loadTodayDrink()
        advanceUntilIdle()

        verify(view).showMessage("No today drink available")
        verify(view).showDashboardData()
    }

    @Test
    fun loadTodayDrink_failure_showsErrorMessage() = runTest(dispatcher) {
        `when`(todayDrinkManager.getTodayDrink()).thenThrow(RuntimeException("Network error"))

        presenter.loadTodayDrink()
        advanceUntilIdle()

        verify(view).showMessage("Failed to load today drink")
        verify(view).showDashboardData()
    }

    @Test
    fun refreshTodayDrink_forcesRefreshAndReloads() = runTest(dispatcher) {
        val todayDrink = Cocktail(
            idDrink = "2",
            strDrink = "Margarita",
            strCategory = "Cocktail",
            strAlcoholic = "Alcoholic"
        )
        `when`(todayDrinkManager.getTodayDrink()).thenReturn(todayDrink)

        presenter.refreshTodayDrink()
        advanceUntilIdle()

        verify(todayDrinkManager).forceRefresh()
        verify(view).showMessage("Refreshed today drink")
        verify(view).showTodayDrink(todayDrink)
    }

    @Test
    fun navigateToCocktailDetail_navigatesToDetail() {
        val cocktail = Cocktail(
            idDrink = "3",
            strDrink = "Negroni",
            strCategory = "Cocktail",
            strAlcoholic = "Alcoholic"
        )

        presenter.navigateToCocktailDetail(cocktail)
        verify(view).navigateToCocktailDetail(cocktail)
    }
}
