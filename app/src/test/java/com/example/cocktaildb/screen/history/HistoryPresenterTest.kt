package com.example.cocktaildb.screen.history

import android.content.Context
import android.os.Looper
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.utils.CocktailContextWrapper
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import android.content.SharedPreferences
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HistoryPresenterTest {

    private lateinit var closeable: AutoCloseable
    private lateinit var context: Context

    @Mock lateinit var cocktailRepository: CocktailRepository
    @Mock lateinit var contextWrapper: CocktailContextWrapper
    @Mock lateinit var view: HistoryContract.View
    @Mock lateinit var historyFirebaseService: HistoryFirebaseService
    @Mock lateinit var authRepository: AuthRepository

    private lateinit var presenter: HistoryPresenter

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        context = mock(Context::class.java)
        
        // Mock SharedPreferences
        val mockPrefs = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.getString(anyString(), anyString())).thenReturn("")
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
        doNothing().`when`(mockEditor).apply()
        
        `when`(contextWrapper.context).thenReturn(context)
        
        // Mock authRepository to return null user to avoid Firebase calls
        `when`(authRepository.getCurrentUser()).thenReturn(null)

        // Create presenter with mocked dependencies
        presenter = HistoryPresenter(cocktailRepository, contextWrapper, historyFirebaseService, authRepository)
        
        presenter.setView(view)

        clearLocalHistory()
    }

    @After
    fun tearDown() {
        if (this::presenter.isInitialized) presenter.onStop()
        clearLocalHistory()
        closeable.close()
    }

    private fun clearLocalHistory() {
        try {
            val sp = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
            sp.edit().clear().apply()
        } catch (e: Exception) {
            // Ignore exceptions in test setup
        }
    }

    private fun putLocalHistory(vararg cocktails: Cocktail) {
        try {
            val sp = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
            val encoded = cocktails.joinToString("||") { c ->
                val ingredients = c.ingredients.joinToString(";;")
                val measures = c.measures.joinToString(";;")
                "${c.idDrink}|:|${c.strDrink}|:|${c.strCategory ?: ""}|:|${c.strAlcoholic ?: ""}|:|${c.strGlass ?: ""}|:|${c.strInstructions ?: ""}|:|${c.strDrinkThumb ?: ""}|:|$ingredients|:|$measures"
            }
            sp.edit().putString("cocktail_history", encoded).apply()
        } catch (e: Exception) {
            // Ignore exceptions in test setup
        }
    }

    @Test
    fun loadHistoryCocktails_offline_emptyLocal_showsEmpty() {
        // Given
        `when`(cocktailRepository.getCheckmarksFromLocal(context)).thenReturn(emptyList())
        
        // When
        presenter.loadHistoryCocktails()

        // Then
        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
        verify(view).showEmptyState()
        verify(view, never()).hideEmptyState()
        verify(view, never()).showHistoryCocktails(anyList())
    }

    @Test
    fun presenter_implements_correct_interface() {
        // Then
        assert(presenter is HistoryContract.Presenter)
    }

    @Test
    fun view_interface_has_required_methods() {
        // Then
        val view: HistoryContract.View = object : HistoryContract.View {
            override fun showHistoryCocktails(cocktails: List<Cocktail>) {}
            override fun showEmptyState() {}
            override fun hideEmptyState() {}
            override fun displayLoading(show: Boolean) {}
            override fun displayError(message: String) {}
            override fun navigateToCocktailDetail(cocktail: Cocktail) {}
            override fun showSyncStatus(message: String) {}
        }
        
        assert(view is HistoryContract.View)
    }
}
