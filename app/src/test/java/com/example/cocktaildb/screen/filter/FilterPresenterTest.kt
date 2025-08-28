package com.example.cocktaildb.screen.filter

import com.example.cocktaildb.data.repository.CocktailRepository
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
class FilterPresenterTest {

    private lateinit var presenter: FilterPresenter
    private lateinit var closeable: AutoCloseable
    private lateinit var view: FilterViewFake
    private lateinit var filterCallback: (String?, String?) -> Unit

    private val dispatcher = StandardTestDispatcher()

    @Mock lateinit var cocktailRepo: CocktailRepository

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        view = FilterViewFake()
        
        filterCallback = { category, alcoholic ->
            // Mock callback implementation
        }
        
        presenter = FilterPresenter(filterCallback, cocktailRepo)
        presenter.setView(view)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    @Test
    fun onCategorySelected_updatesSelectedCategory() {
        val category = "Cocktail"
        
        presenter.onCategorySelected(category)
        
        assert(view.lastCategory == category)
    }

    @Test
    fun onAlcoholicSelected_updatesSelectedAlcoholic() {
        val alcoholic = "Alcoholic"
        
        presenter.onAlcoholicSelected(alcoholic)
        
        assert(view.lastAlcoholic == alcoholic)
    }

    @Test
    fun onFilterApplied_invokesCallbackAndDismissesDialog() {
        val category = "Cocktail"
        val alcoholic = "Alcoholic"
        
        // Set selections first
        presenter.onCategorySelected(category)
        presenter.onAlcoholicSelected(alcoholic)
        
        // Apply filter
        presenter.onFilterApplied()
        
        assert(view.isDismissed)
    }

    @Test
    fun onFilterApplied_withNullSelections_invokesCallbackWithNulls() {
        var capturedCategory: String? = null
        var capturedAlcoholic: String? = null
        
        val testCallback: (String?, String?) -> Unit = { category, alcoholic ->
            capturedCategory = category
            capturedAlcoholic = alcoholic
        }
        
        val testPresenter = FilterPresenter(testCallback, cocktailRepo)
        testPresenter.setView(view)
        
        testPresenter.onFilterApplied()
        
        assert(capturedCategory == null)
        assert(capturedAlcoholic == null)
        assert(view.isDismissed)
    }

    @Test
    fun onFilterApplied_withSelections_invokesCallbackWithSelections() {
        var capturedCategory: String? = null
        var capturedAlcoholic: String? = null
        
        val testCallback: (String?, String?) -> Unit = { category, alcoholic ->
            capturedCategory = category
            capturedAlcoholic = alcoholic
        }
        
        val testPresenter = FilterPresenter(testCallback, cocktailRepo)
        testPresenter.setView(view)
        
        val category = "Cocktail"
        val alcoholic = "Alcoholic"
        
        testPresenter.onCategorySelected(category)
        testPresenter.onAlcoholicSelected(alcoholic)
        testPresenter.onFilterApplied()
        
        assert(capturedCategory == category)
        assert(capturedAlcoholic == alcoholic)
        assert(view.isDismissed)
    }

    @Test
    fun setView_nullView_clearsView() {
        presenter.setView(null)
        
        // Should not crash when calling methods
        presenter.onCategorySelected("test")
        // No verification needed as view is null
    }

    @Test
    fun onStop_clearsView() {
        presenter.onStop()
        
        // Should not crash when calling methods
        presenter.onCategorySelected("test")
        // No verification needed as view is null
    }

    private class FilterViewFake : FilterContract.View {
        var lastCategory: String? = null
        var lastAlcoholic: String? = null
        var lastCategories: List<String>? = null
        var lastAlcoholicTypes: List<String>? = null
        var lastError: String? = null
        var isLoading = false
        var isDismissed = false

        override fun showLoading() {
            isLoading = true
        }

        override fun hideLoading() {
            isLoading = false
        }

        override fun showCategories(categories: List<String>) {
            lastCategories = categories
        }

        override fun showAlcoholicTypes(alcoholicTypes: List<String>) {
            lastAlcoholicTypes = alcoholicTypes
        }

        override fun updateSelectedCategory(category: String) {
            lastCategory = category
        }

        override fun updateSelectedAlcoholic(alcoholic: String) {
            lastAlcoholic = alcoholic
        }

        override fun showError(error: String) {
            lastError = error
        }

        override fun showMessage(message: String) {
            // Implementation for showMessage
        }

        override fun dismissDialog() {
            isDismissed = true
        }
    }
}
