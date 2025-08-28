package com.example.cocktaildb.screen.myrecipe

import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class MyRecipePresenterTest {

    private lateinit var presenter: MyRecipePresenter
    private lateinit var closeable: AutoCloseable

    private val dispatcher = StandardTestDispatcher()

    @Mock lateinit var recipeService: RecipeFirebaseService
    @Mock lateinit var authRepo: AuthRepository
    @Mock lateinit var mockUser: FirebaseUser
    @Mock lateinit var view: MyRecipeContract.View

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(dispatcher)
        
        presenter = MyRecipePresenter(
            recipeFirebaseService = recipeService,
            authRepository = authRepo,
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
    fun loadUserRecipes_noUser_showsError() = runTest(dispatcher) {
        // Reset to clear any interactions from setUp
        reset(view)
        
        `when`(authRepo.getCurrentUser()).thenReturn(null)

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayError("Please log in to view your recipes")
        verify(view, never()).displayLoading(eq(true))
        verify(view, never()).displayLoading(eq(false))
        verify(view, never()).showUserRecipes(anyList())
    }

    @Test
    fun loadUserRecipes_success_showsRecipes() = runTest(dispatcher) {
        // Reset to clear any interactions from setUp
        reset(view)
        
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
        verify(view, never()).displayError(anyString())
    }

    @Test
    fun loadUserRecipes_failure_showsErrorMessage() = runTest(dispatcher) {
        // Reset to clear any interactions from setUp
        reset(view)
        
        `when`(authRepo.getCurrentUser()).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test_user_id")
        `when`(recipeService.getUserRecipes("test_user_id")).thenReturn(Result.failure(RuntimeException("Network error")))

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).displayError("Failed to load recipes: Network error")
        verify(view, never()).showUserRecipes(anyList())
    }

    @Test
    fun loadUserRecipes_exception_showsErrorMessage() = runTest(dispatcher) {
        // Reset to clear any interactions from setUp
        reset(view)
        
        `when`(authRepo.getCurrentUser()).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("test_user_id")
        `when`(recipeService.getUserRecipes("test_user_id")).thenThrow(RuntimeException("Database error"))

        presenter.loadUserRecipes()
        advanceUntilIdle()

        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).displayError("Error loading recipes: Database error")
        verify(view, never()).showUserRecipes(anyList())
    }

    @Test
    fun refreshUserRecipes_resetsCacheAndReloads() = runTest(dispatcher) {
        // Reset to clear any interactions from setUp
        reset(view)
        
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

        // Reset view interactions
        reset(view)
        
        // Create new view mock
        val newView = mock(MyRecipeContract.View::class.java)
        presenter.setView(newView)

        // Should show cached data immediately
        verify(newView).showUserRecipes(recipes)
    }

    @Test
    fun onStop_cancelsJobAndClearsView() {
        reset(view)
        
        presenter.onStop()

        // Should not crash when calling methods
        presenter.loadUserRecipes()

        verifyNoInteractions(view)
    }
} 
