package com.example.cocktaildb.screen.favorites

import android.content.Context
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FavoritesPresenterTest {

    private lateinit var context: Context
    private lateinit var presenter: FavoritesPresenter
    private lateinit var closeable: AutoCloseable

    private val dispatcher = StandardTestDispatcher()

    @Mock lateinit var repo: CocktailRepository
    @Mock lateinit var view: FavoritesContract.View
    @Mock lateinit var mockAuth: FirebaseAuth
    @Mock lateinit var mockUser: FirebaseUser

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()

        // Setup Firebase auth mock
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test_user_id")

        presenter = FavoritesPresenter(
            context = context,
            cocktailRepository = repo,
            firebaseAuth = mockAuth,
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
    fun loadFavorites_offline_localHasItems_showsList() = runTest(dispatcher) {
        val fakeList = listOf(
            Cocktail(
                idDrink = "1",
                strDrink = "Mojito",
                strCategory = "Cocktail",
                strAlcoholic = "Alcoholic",
                strGlass = "Glass",
                strInstructions = "Mix ingredients"
            )
        )
        `when`(repo.getFavoritesFromLocal(context)).thenReturn(fakeList)

        presenter.loadFavorites()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view).displayFavorites(fakeList)
        verify(view).displayLoading(false)
    }

    @Test
    fun loadFavorites_offline_localEmpty_showsEmpty() = runTest(dispatcher) {
        `when`(repo.getFavoritesFromLocal(context)).thenReturn(emptyList())

        presenter.loadFavorites()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view).displayEmptyState()
        verify(view).displayLoading(false)
    }

    @Test
    fun toggleFavorite_togglesAndReloadsLocal() = runTest(dispatcher) {
        val cocktail = Cocktail(
            idDrink = "9",
            strDrink = "Negroni",
            strCategory = "Cocktail",
            strAlcoholic = "Alcoholic",
            strGlass = "Glass",
            strInstructions = "Mix ingredients"
        )

        `when`(repo.toggleFavorite(context, cocktail)).thenReturn(true)
        `when`(repo.getFavoritesFromLocal(context)).thenReturn(listOf(cocktail))

        presenter.toggleFavorite(cocktail)
        advanceUntilIdle()

        verify(repo).toggleFavorite(context, cocktail)
        verify(view).displayFavorites(listOf(cocktail))
    }

    @Test
    fun addToFavorites_success_showsStatusAndReloadsLocal() = runTest(dispatcher) {
        val cocktail = Cocktail(
            idDrink = "2",
            strDrink = "Margarita",
            strCategory = "Cocktail",
            strAlcoholic = "Alcoholic",
            strGlass = "Glass",
            strInstructions = "Shake ingredients"
        )

        // Use doAnswer to capture and invoke the callback
        doAnswer { invocation ->
            val callback = invocation.arguments[2] as (Boolean) -> Unit
            callback(true)
            null
        }.`when`(repo).addToFavorites(context, cocktail, any())
        
        `when`(repo.getFavoritesFromLocal(context)).thenReturn(listOf(cocktail))

        presenter.addToFavorites(cocktail)
        advanceUntilIdle()

        verify(view).showSyncStatus("Added to favorites")
        verify(view).displayFavorites(listOf(cocktail))
    }

    @Test
    fun clearAllFavorites_offline_clearsLocalAndShowsEmpty() = runTest(dispatcher) {
        doNothing().`when`(repo).clearAllFavorites(context)

        presenter.clearAllFavorites()
        advanceUntilIdle()

        verify(repo).clearAllFavorites(context)
        verify(view).showSyncStatus("Local favorites cleared")
        verify(view).displayEmptyState()
    }
}
