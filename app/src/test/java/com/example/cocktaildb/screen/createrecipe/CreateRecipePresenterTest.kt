package com.example.cocktaildb.screen.createrecipe

import android.content.Context
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher(), TestRule {
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CreateRecipePresenterTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var closeable: AutoCloseable
    private lateinit var context: Context

    @Mock lateinit var firebaseRepository: FirebaseRepository
    @Mock lateinit var authRepository: AuthRepository
    @Mock lateinit var view: CreateRecipeContract.View
    @Mock lateinit var mockUser: FirebaseUser
    @Mock lateinit var contextMock: Context

    private lateinit var presenter: CreateRecipePresenter

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)

        // Mock context.getString
        `when`(contextMock.getString(R.string.error_recipe_name_empty))
            .thenReturn("Recipe name cannot be empty")
        `when`(contextMock.getString(R.string.error_instructions_empty))
            .thenReturn("Instructions cannot be empty")
        `when`(contextMock.getString(R.string.error_add_at_least_one_ingredient))
            .thenReturn("Add at least one ingredient")
        `when`(contextMock.getString(R.string.error_user_not_authenticated))
            .thenReturn("User not authenticated")
        `when`(contextMock.getString(eq(R.string.error_failed_to_save_recipe), any()))
            .thenReturn("Failed to save recipe")

        presenter = CreateRecipePresenter(contextMock, firebaseRepository, authRepository)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        presenter.onStop()
        closeable.close()
    }

    @Test
    fun saveRecipe_showsError_whenNameBlank() = runTest {
        presenter.saveRecipe("", "mix well", "", listOf("Rum"), listOf("50ml"), "Cocktail", "Highball", true)

        verify(view).showError(anyString())
        verify(view, never()).showLoading(true)
        verifyNoInteractions(firebaseRepository)
    }

    @Test
    fun saveRecipe_showsError_whenInstructionsBlank() = runTest {
        presenter.saveRecipe(
            name = "Mojito",
            instructions = "",
            imageUrl = "",
            ingredients = listOf("Rum"),
            measures = listOf("50ml"),
            category = "Cocktail",
            glass = "Highball",
            alcoholic = true
        )
        verify(view).showError(anyString())
        verify(view, never()).showLoading(true)
        verifyNoInteractions(firebaseRepository)
    }

    @Test
    fun saveRecipe_showsError_whenIngredientsInvalid() = runTest {
        presenter.saveRecipe("Mojito", "mix well", "", emptyList(), emptyList(), "Cocktail", "Highball", true)

        verify(view).showError(anyString())
        verify(view, never()).showLoading(true)
        verifyNoInteractions(firebaseRepository)
    }

    @Test
    fun presenter_implements_correct_interface() {
        assert(presenter is CreateRecipeContract.Presenter)
    }

    @Test
    fun presenter_has_correct_package() {
        val packageName = presenter::class.java.`package`.name
        assert(packageName.contains("createrecipe"))
    }
}
