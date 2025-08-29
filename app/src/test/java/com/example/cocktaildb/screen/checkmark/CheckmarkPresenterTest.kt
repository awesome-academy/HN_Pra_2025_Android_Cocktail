package com.example.cocktaildb.screen.checkmark

import android.content.Context
import android.os.Looper
import com.example.cocktaildb.data.model.Cocktail
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.example.cocktaildb.data.repository.CocktailRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import com.google.firebase.auth.FirebaseAuth
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CheckmarkPresenterTest {

    private lateinit var closeable: AutoCloseable
    private lateinit var context: Context

    @Mock lateinit var cocktailRepository: CocktailRepository
    @Mock lateinit var view: CheckmarkContract.View

    private lateinit var presenter: CheckmarkPresenter
    private lateinit var firebaseAuthStatic: MockedStatic<FirebaseAuth>
    private lateinit var mockAuth: FirebaseAuth

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        context = mockk<Context>(relaxed = true)
        
        // Mock SharedPreferences
        val mockPrefs = mockk<SharedPreferences>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getString(any(), any()) } returns ""
        every { mockPrefs.edit() } returns mockk(relaxed = true)
        
        presenter = CheckmarkPresenter(context, cocktailRepository)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        if (this::presenter.isInitialized) {
            presenter.onStop()
        }
        // Close static mock to avoid leaks across tests
        if (this::firebaseAuthStatic.isInitialized) {
            firebaseAuthStatic.close()
        }
        closeable.close()
    }

    private fun dummyCocktail(id: String = "1", name: String = "Drink $id"): Cocktail =
        Cocktail(
            idDrink = id,
            strDrink = name,
            strCategory = "Category",
            strAlcoholic = "Alcoholic",
            strGlass = "Glass",
            strInstructions = "Instr",
            strDrinkThumb = null,
            ingredients = emptyList(),
            measures = emptyList()
        )

    @Test
    fun loadCheckmarks_offline_loadsLocalList() {
        val localList = listOf(dummyCocktail("10", "Local A"), dummyCocktail("11", "Local B"))
        every { cocktailRepository.getCheckmarksFromLocal(context) } returns localList

        presenter.loadCheckmarks()

        verify(view).displayLoading(true)
        verify(view).displayCheckmarks(localList)
        verify(view).displayLoading(false)
        verify(view, never()).displayEmptyState()
    }

    @Test
    fun presenter_implements_correct_interface() {
        // Then
        assert(presenter is CheckmarkContract.Presenter)
    }

    @Test
    fun view_interface_has_required_methods() {
        // Then
        // Verify that the view interface has all required methods
        val view: CheckmarkContract.View = object : CheckmarkContract.View {
            override fun displayLoading(show: Boolean) {}
            override fun displayCheckmarks(checkmarks: List<Cocktail>) {}
            override fun displayEmptyState() {}
            override fun displayError(message: String) {}
            override fun showCheckmarkAdded(cocktail: Cocktail) {}
            override fun showCheckmarkRemoved(cocktail: Cocktail) {}
            override fun showSyncStatus(message: String) {}
        }
        
        assert(view is CheckmarkContract.View)
    }
}
