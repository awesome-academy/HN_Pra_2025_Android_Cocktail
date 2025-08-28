package com.example.cocktaildb.screen.myrecipe

import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class MyRecipePresenterTest {

    private lateinit var presenter: MyRecipePresenter
    private lateinit var closeable: AutoCloseable
    private lateinit var view: MyRecipeViewFake

    private val dispatcher = StandardTestDispatcher()

    @Mock lateinit var recipeService: RecipeFirebaseService
    @Mock lateinit var authRepo: AuthRepository
    @Mock lateinit var mockUser: FirebaseUser

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        view = MyRecipeViewFake()
        
        presenter = MyRecipePresenter(recipeService, authRepo)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    @Test
    fun loadUserRecipes_noUser_showsError() = runTest(dispatcher) {
        `when`(authRepo.getCurrentUser()).thenReturn(null)

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayError("Please log in to view your recipes")
        verify(view, never()).displayLoading(any())
    }

    @Test
    fun loadUserRecipes_success_showsRecipes() = runTest(dispatcher) {
        val recipes = listOf(
            Recipe(
                id = "1",
                name = "Custom Mojito",
                category = "Custom",
                alcoholic = "Alcoholic",
                instructions = "Mix ingredients"
            ),
            Recipe(
                id = "2",
                name = "Custom Margarita",
                category = "Custom",
                alcoholic = "Alcoholic",
                instructions = "Shake ingredients"
            )
        )
        
        `when`(authRepo.getCurrentUser()).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test_user_id")
        `when`(recipeService.getUserRecipes("test_user_id")).thenReturn(Result.success(recipes))

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).showUserRecipes(recipes)
        verify(view, never()).displayError(any())
    }

    @Test
    fun loadUserRecipes_failure_showsErrorMessage() = runTest(dispatcher) {
        `when`(authRepo.getCurrentUser()).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test_user_id")
        `when`(recipeService.getUserRecipes("test_user_id")).thenReturn(Result.failure(RuntimeException("Network error")))

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).displayError("Failed to load recipes: Network error")
        verify(view, never()).showUserRecipes(any())
    }

    @Test
    fun loadUserRecipes_exception_showsErrorMessage() = runTest(dispatcher) {
        `when`(authRepo.getCurrentUser()).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test_user_id")
        `when`(recipeService.getUserRecipes("test_user_id")).thenThrow(RuntimeException("Database error"))

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).displayError("Error loading recipes: Database error")
        verify(view, never()).showUserRecipes(any())
    }

    @Test
    fun refreshUserRecipes_resetsCacheAndReloads() = runTest(dispatcher) {
        val recipes = listOf(
            Recipe(
                id = "1",
                name = "Custom Mojito",
                category = "Custom",
                alcoholic = "Alcoholic",
                instructions = "Mix ingredients"
            )
        )
        
        `when`(authRepo.getCurrentUser()).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test_user_id")
        `when`(recipeService.getUserRecipes("test_user_id")).thenReturn(Result.success(recipes))

        presenter.refreshUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).showUserRecipes(recipes)
    }

    @Test
    fun setView_withCachedData_showsCachedRecipes() = runTest(dispatcher) {
        val recipes = listOf(
            Recipe(
                id = "1",
                name = "Custom Mojito",
                category = "Custom",
                alcoholic = "Alcoholic",
                instructions = "Mix ingredients"
            )
        )
        
        `when`(authRepo.getCurrentUser()).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test_user_id")
        `when`(recipeService.getUserRecipes("test_user_id")).thenReturn(Result.success(recipes))

        // Load data first to cache it
        presenter.loadUserRecipes()
        advanceUntilIdle()

        // Create new view
        val newView = MyRecipeViewFake()
        presenter.setView(newView)

        // Should show cached data immediately
        verify(newView).showUserRecipes(recipes)
    }

    @Test
    fun onStop_cancelsJobAndClearsView() {
        presenter.onStop()

        // Should not crash when calling methods
        presenter.loadUserRecipes()
        // No verification needed as view is null
    }

    private class MyRecipeViewFake : MyRecipeContract.View {
        var lastRecipes: List<Recipe>? = null
        var lastError: String? = null
        var isLoading = false

        override fun displayLoading(show: Boolean) {
            isLoading = show
        }

        override fun showUserRecipes(recipes: List<Recipe>) {
            lastRecipes = recipes
        }

        override fun displayError(error: String) {
            lastError = error
        }
    }
} 
