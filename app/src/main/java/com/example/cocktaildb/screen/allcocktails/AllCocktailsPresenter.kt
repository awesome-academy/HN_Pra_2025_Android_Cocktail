package com.example.cocktaildb.screen.allcocktails

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.utils.base.BasePresenter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllCocktailsPresenter(
    private val cocktailRepository: CocktailRepository,
    private val authRepository: AuthRepository,
    private val historyFirebaseService: HistoryFirebaseService,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BasePresenter<AllCocktailsContract.View> {

    private var view: AllCocktailsContract.View? = null
    private val coroutineScope = CoroutineScope(mainDispatcher)
    
    private var allCocktails: MutableList<Cocktail> = mutableListOf()
    private var filteredCocktails: MutableList<Cocktail> = mutableListOf()
    private var currentQuery: String = ""
    private var currentFilterCategory: String? = null
    private var currentFilterAlcoholic: String? = null
    
    private val pageSize = 10
    private var currentPage = 1
    private var totalPages = 1
    
    // Simple cache - static để share giữa các instance
    companion object {
        private var cachedCocktails: List<Cocktail>? = null
        private var lastCacheTime: Long = 0
        private const val CACHE_DURATION_MS = 10 * 60 * 1000L // 10 minutes
    }

    override fun setView(view: AllCocktailsContract.View?) {
        android.util.Log.d("AllCocktailsPresenter", "setView called with view: ${view != null}")
        this.view = view
    }

    override fun onStart() {
        android.util.Log.d("AllCocktailsPresenter", "onStart called")
        // Initialize if needed
    }

    override fun onStop() {
        // Clean up if needed
    }

        fun loadAllCocktails() {
        android.util.Log.d("AllCocktailsPresenter", "loadAllCocktails called, view: ${view != null}")
        
        // Check cache first
        val currentTime = System.currentTimeMillis()
        val isCacheValid = cachedCocktails != null && (currentTime - lastCacheTime) < CACHE_DURATION_MS
        
        if (isCacheValid && cachedCocktails != null) {
            android.util.Log.d("AllCocktailsPresenter", "Using cached cocktails: ${cachedCocktails!!.size}")
            allCocktails = cachedCocktails!!.toMutableList()
            filteredCocktails = cachedCocktails!!.toMutableList()
            updatePagination()
            val currentPageCocktails = getCurrentPageCocktails()
            android.util.Log.d("AllCocktailsPresenter", "Showing ${currentPageCocktails.size} cocktails from cache on page $currentPage")
            view?.showCocktails(currentPageCocktails)
            return
        }
        
        // Load from API if cache is invalid or empty
        android.util.Log.d("AllCocktailsPresenter", "Cache invalid or empty, loading from API")
        view?.showLoadingState()
        coroutineScope.launch {
            try {
                val cocktails = withContext(ioDispatcher) {
                    cocktailRepository.getAllCocktails()
                }
                android.util.Log.d("AllCocktailsPresenter", "Loaded ${cocktails.size} cocktails from API")
                
                // Cache the results
                cachedCocktails = cocktails
                lastCacheTime = currentTime
                android.util.Log.d("AllCocktailsPresenter", "Cached ${cocktails.size} cocktails for future use")
                
                allCocktails = cocktails.toMutableList()
                filteredCocktails = cocktails.toMutableList()
                updatePagination()
                val currentPageCocktails = getCurrentPageCocktails()
                android.util.Log.d("AllCocktailsPresenter", "Showing ${currentPageCocktails.size} cocktails on page $currentPage")
                view?.showCocktails(currentPageCocktails)
                view?.hideLoadingState()
            } catch (e: Exception) {
                android.util.Log.e("AllCocktailsPresenter", "Error loading cocktails", e)
                view?.hideLoadingState()
                view?.showErrorMessage("Failed to load cocktails: ${e.message}")
            }
                }
    }
    
    fun loadMoreCocktails() {
        // Load additional cocktails for pagination
        coroutineScope.launch {
            try {
                val moreCocktails = withContext(ioDispatcher) {
                    cocktailRepository.loadMoreCocktails()
                }
                android.util.Log.d("AllCocktailsPresenter", "Loaded ${moreCocktails.size} additional cocktails")
                
                allCocktails.addAll(moreCocktails)
                filteredCocktails = allCocktails.toMutableList()
                updatePagination()
                view?.showMessage("Loaded ${moreCocktails.size} more cocktails")
            } catch (e: Exception) {
                android.util.Log.e("AllCocktailsPresenter", "Error loading more cocktails", e)
                view?.showErrorMessage("Error loading more cocktails: ${e.message}")
            }
        }
    }
    
    fun searchCocktails(query: String) {
        currentQuery = query
        currentFilterCategory = null
        currentFilterAlcoholic = null
        
        if (query.isEmpty()) {
            filteredCocktails = allCocktails.toMutableList()
        } else {
            filteredCocktails = allCocktails.filter { cocktail ->
                cocktail.strDrink.contains(query, ignoreCase = true) ||
                cocktail.strCategory?.contains(query, ignoreCase = true) == true ||
                cocktail.strAlcoholic?.contains(query, ignoreCase = true) == true
            }.toMutableList()
        }
        
        currentPage = 1
        updatePagination()
        view?.showCocktails(getCurrentPageCocktails())
    }

    fun filterByCategory(category: String) {
        currentFilterCategory = category
        currentFilterAlcoholic = null
        
        filteredCocktails = allCocktails.filter { cocktail ->
            (category.isEmpty() || cocktail.strCategory == category) &&
            (currentQuery.isEmpty() || cocktail.strDrink.contains(currentQuery, ignoreCase = true))
        }.toMutableList()
        
        currentPage = 1
        updatePagination()
        view?.showCocktails(getCurrentPageCocktails())
    }

    fun filterByAlcoholic(alcoholicType: String) {
        currentFilterAlcoholic = alcoholicType
        currentFilterCategory = null
        
        filteredCocktails = allCocktails.filter { cocktail ->
            (alcoholicType.isEmpty() || cocktail.strAlcoholic == alcoholicType) &&
            (currentQuery.isEmpty() || cocktail.strDrink.contains(currentQuery, ignoreCase = true))
        }.toMutableList()
        
        currentPage = 1
        updatePagination()
        view?.showCocktails(getCurrentPageCocktails())
    }

    fun onCocktailClicked(cocktail: Cocktail) {
        // Save to history
        coroutineScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    withContext(ioDispatcher) {
                        historyFirebaseService.addHistory(currentUser.uid, cocktail.idDrink)
                    }
                }
            } catch (e: Exception) {
                // Log error but don't show to user
            }
        }
        
        view?.navigateToCocktailDetail(cocktail)
    }

    fun nextPage() {
        if (currentPage < totalPages) {
            currentPage++
            view?.showCocktails(getCurrentPageCocktails())
            updatePagination()
        }
    }

    fun previousPage() {
        if (currentPage > 1) {
            currentPage--
            view?.showCocktails(getCurrentPageCocktails())
            updatePagination()
        }
    }

    fun goToPage(page: Int) {
        if (page in 1..totalPages) {
            currentPage = page
            view?.showCocktails(getCurrentPageCocktails())
            updatePagination()
        }
    }

    private fun getCurrentPageCocktails(): List<Cocktail> {
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, filteredCocktails.size)
        return if (startIndex < filteredCocktails.size) {
            filteredCocktails.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    private fun updatePagination() {
        totalPages = (filteredCocktails.size + pageSize - 1) / pageSize
        if (totalPages == 0) totalPages = 1
        
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
        if (currentPage < 1) currentPage = 1
        
        val hasNext = currentPage < totalPages
        val hasPrevious = currentPage > 1
        
        android.util.Log.d("AllCocktailsPresenter", "Pagination: total=${filteredCocktails.size}, pageSize=$pageSize, totalPages=$totalPages, currentPage=$currentPage")
        
        view?.updatePagination(currentPage, totalPages, hasNext, hasPrevious)
        view?.showPagination(totalPages > 1)
    }
    
    fun clearCache() {
        cachedCocktails = null
        lastCacheTime = 0
        android.util.Log.d("AllCocktailsPresenter", "Cache cleared")
    }
} 

