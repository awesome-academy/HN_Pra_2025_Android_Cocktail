package com.example.cocktaildb.screen.search

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.data.service.SearchHistoryService
import com.example.cocktaildb.utils.base.BasePresenter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchPresenter(
    private val cocktailRepository: CocktailRepository,
    private val authRepository: AuthRepository,
    private val historyFirebaseService: HistoryFirebaseService,
    private val searchHistoryService: SearchHistoryService,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BasePresenter<SearchContract.View> {

    private var view: SearchContract.View? = null
    private val presenterScope = CoroutineScope(mainDispatcher + Job())

    private var allCocktails: List<Cocktail> = emptyList()
    private var filteredCocktails: List<Cocktail> = emptyList()
    private var currentQuery: String = ""
    private var currentFilterCategory: String? = null
    private var currentFilterAlcoholic: String? = null

    private val pageSize = 10
    private var currentPage = 1
    private var totalPages = 1

    private var searchJob: Job? = null

    override fun setView(view: SearchContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // Initialize if needed
    }

    override fun onStop() {
        searchJob?.cancel()
        presenterScope.cancel()
    }

    fun searchCocktails(query: String) {
        currentQuery = query
        currentFilterCategory = null
        currentFilterAlcoholic = null

        view?.showLoading()

        presenterScope.launch {
            try {
                val cocktails = if (query.isEmpty()) {
                    withContext(ioDispatcher) {
                        cocktailRepository.getCocktails()
                    }
                } else {
                    withContext(ioDispatcher) {
                        cocktailRepository.searchCocktails(query)
                    }
                }

                allCocktails = cocktails
                filteredCocktails = cocktails
                updatePagination()
                val currentPageCocktails = getCurrentPageCocktails()
                view?.showCocktails(currentPageCocktails)
            } catch (e: Exception) {
                Log.e("SearchPresenter", "Error searching cocktails", e)
                view?.showError("Error searching cocktails: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    fun filterByCategory(category: String) {
        currentFilterCategory = category
        currentFilterAlcoholic = null

        view?.showLoading()

        presenterScope.launch {
            try {
                val filtered = if (category.isEmpty()) {
                    allCocktails
                } else {
                    withContext(ioDispatcher) {
                        cocktailRepository.filterByCategory(category)
                    }
                }

                filteredCocktails = filtered
                currentPage = 1
                updatePagination()
                val currentPageCocktails = getCurrentPageCocktails()
                view?.showCocktails(currentPageCocktails)
            } catch (e: Exception) {
                Log.e("SearchPresenter", "Error filtering by category", e)
                view?.showError("Error filtering by category: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    fun filterByAlcoholic(alcoholicType: String) {
        currentFilterAlcoholic = alcoholicType
        currentFilterCategory = null

        view?.showLoading()

        presenterScope.launch {
            try {
                val filtered = if (alcoholicType.isEmpty()) {
                    allCocktails
                } else {
                    withContext(ioDispatcher) {
                        cocktailRepository.filterByAlcoholic(alcoholicType)
                    }
                }

                filteredCocktails = filtered
                currentPage = 1
                updatePagination()
                val currentPageCocktails = getCurrentPageCocktails()
                view?.showCocktails(currentPageCocktails)
            } catch (e: Exception) {
                Log.e("SearchPresenter", "Error filtering by alcoholic", e)
                view?.showError("Error filtering by alcoholic: ${e.message}")
            } finally {
                view?.hideLoading()
            }
        }
    }

    fun onSearchTextChanged(query: String) {
        searchJob?.cancel()
        searchJob = presenterScope.launch {
            delay(300) // Debounce 300ms
            if (query.isNotEmpty()) {
                try {
                    val suggestions = withContext(ioDispatcher) {
                        searchHistoryService.getSuggestions(query)
                    }
                    view?.showSearchSuggestions(suggestions)
                } catch (e: Exception) {
                    Log.e("SearchPresenter", "Error getting suggestions", e)
                }
            } else {
                view?.hideSearchSuggestions()
            }
        }
    }

    fun onSearchSubmitted(query: String) {
        searchJob?.cancel()
        view?.hideSearchSuggestions()

        presenterScope.launch {
            try {
                withContext(ioDispatcher) {
                    searchHistoryService.addSearchQuery(query)
                }
                searchCocktails(query)
            } catch (e: Exception) {
                Log.e("SearchPresenter", "Error submitting search", e)
            }
        }
    }

    fun onSuggestionClicked(suggestion: String) {
        view?.updateSearchText(suggestion)
        view?.hideSearchSuggestions()
        onSearchSubmitted(suggestion)
    }

    fun onSuggestionRemoved(suggestion: String) {
        presenterScope.launch {
            try {
                withContext(ioDispatcher) {
                    searchHistoryService.removeSearchQuery(suggestion)
                }
                // Refresh suggestions if needed
                val currentQuery = currentQuery
                if (currentQuery.isNotEmpty()) {
                    val suggestions = withContext(ioDispatcher) {
                        searchHistoryService.getSuggestions(currentQuery)
                    }
                    view?.showSearchSuggestions(suggestions)
                }
            } catch (e: Exception) {
                Log.e("SearchPresenter", "Error removing suggestion", e)
            }
        }
    }

    fun onSearchFocused() {
        // Load recent searches when search is focused
        presenterScope.launch {
            try {
                val recentSearches = withContext(ioDispatcher) {
                    searchHistoryService.getRecentSearches()
                }
                if (recentSearches.isNotEmpty()) {
                    view?.showSearchSuggestions(recentSearches)
                }
            } catch (e: Exception) {
                Log.e("SearchPresenter", "Error loading recent searches", e)
            }
        }
    }

    fun onCocktailClicked(cocktail: Cocktail) {
        presenterScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    withContext(ioDispatcher) {
                        historyFirebaseService.addHistory(currentUser.uid, cocktail.idDrink)
                    }
                }
            } catch (e: Exception) {
                Log.e("SearchPresenter", "Error adding to history", e)
            }
        }

        view?.navigateToCocktailDetail(cocktail)
    }

    fun nextPage() {
        if (currentPage < totalPages) {
            currentPage++
            val currentPageCocktails = getCurrentPageCocktails()
            view?.showCocktails(currentPageCocktails)
            updatePagination()
        }
    }

    fun previousPage() {
        if (currentPage > 1) {
            currentPage--
            val currentPageCocktails = getCurrentPageCocktails()
            view?.showCocktails(currentPageCocktails)
            updatePagination()
        }
    }

    fun goToPage(page: Int) {
        if (page in 1..totalPages) {
            currentPage = page
            val currentPageCocktails = getCurrentPageCocktails()
            view?.showCocktails(currentPageCocktails)
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

        view?.updatePagination(currentPage, totalPages, hasNext, hasPrevious)
        view?.showPagination(totalPages > 1)
    }
}


