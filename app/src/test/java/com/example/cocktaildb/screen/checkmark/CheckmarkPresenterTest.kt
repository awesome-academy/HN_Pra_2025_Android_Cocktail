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
        context = RuntimeEnvironment.getApplication()

        // Mock static FirebaseAuth.getInstance() to avoid real Firebase components
        firebaseAuthStatic = mockStatic(FirebaseAuth::class.java)
        mockAuth = mock(FirebaseAuth::class.java)
        firebaseAuthStatic.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockAuth)
        `when`(mockAuth.currentUser).thenReturn(null) // Force offline branch

        // Optional: initialize FirebaseApp to satisfy any internal checks (not strictly needed with static mock)
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:abcdef123456")
                .setApiKey("fake-api-key")
                .setProjectId("test-project")
                .build()
            FirebaseApp.initializeApp(context, options)
        }

        presenter = CheckmarkPresenter(context, cocktailRepository)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        presenter.onStop()
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
        `when`(cocktailRepository.getCheckmarksFromLocal(context)).thenReturn(localList)

        presenter.loadCheckmarks()

        verify(view).displayLoading(true)
        verify(view).displayCheckmarks(localList)
        verify(view).displayLoading(false)
        verify(view, never()).displayEmptyState()
    }

    @Test
    fun addToCheckmarks_offline_savesToLocal() {
        val c = dummyCocktail("20", "New One")

        presenter.addToCheckmarks(c)

        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Cocktail>>
        verify(cocktailRepository, atLeastOnce()).saveCheckmarksToLocal(eq(context), captor.capture())
        // Verify that the saved list contains our cocktail
        assert(captor.allValues.any { list -> list.any { it.idDrink == c.idDrink } })
        // Offline branch does not notify view about added checkmark explicitly
        verify(view, never()).showCheckmarkAdded(any())
    }

    @Test
    fun removeFromCheckmarks_offline_updatesLocalAndView() {
        val c = dummyCocktail("30", "To Remove")
        // Local initially has the cocktail
        `when`(cocktailRepository.getCheckmarksFromLocal(context)).thenReturn(listOf(c))

        presenter.removeFromCheckmarks(c)

        // Let coroutine run and post back to main
        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // After removal, local list becomes empty -> showEmptyState
        verify(view).showCheckmarkRemoved(c)
        verify(view).displayEmptyState()
    }

    @Test
    fun clearAllCheckmarks_offline_showsEmptyAndSyncStatus() {
        presenter.clearAllCheckmarks()

        Thread.sleep(100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(cocktailRepository).clearAllCheckmarksFromLocal(context)
        verify(view).displayEmptyState()
        verify(view).showSyncStatus(anyString())
    }
}
