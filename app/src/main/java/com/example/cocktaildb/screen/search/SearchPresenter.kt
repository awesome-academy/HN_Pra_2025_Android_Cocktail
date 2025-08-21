package com.example.cocktaildb.screen.search

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.utils.pagination.PaginationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import java.util.concurrent.Executors

class SearchPresenter(
    private val cocktailRepository: CocktailRepository,
    private val authRepository: AuthRepository,
    private val historyFirebaseService: HistoryFirebaseService
) : SearchContract.Presenter {

    private var view: SearchContract.View? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentSearchQuery: String = ""
    private val paginationData = PaginationData<Cocktail>(10)

    override fun setView(view: SearchContract.View?) {
        this.view = view
    }

    override fun onStart() {
        searchCocktails("")
    }

    override fun onStop() {
        presenterScope.cancel()
        view = null
    }

    override fun searchCocktails(query: String) {
        currentSearchQuery = query
        view?.showLoading()
        executor.execute {
            try {
                val cocktails = if (query.isEmpty()) {
                    cocktailRepository.getCocktails()
                } else {
                    cocktailRepository.searchCocktails(query)
                }
                
                paginationData.setData(cocktails)
                
                view?.let { v ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        v.hideLoading()
                        v.showCocktails(paginationData.currentPageItems)
                        updatePaginationUI()
                    }
                }
            } catch (e: Exception) {
                view?.let { v ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        v.hideLoading()
                        v.showError("Error loading cocktails: ${e.message}")
                    }
                }
            }
        }
    }

    override fun filterByCategory(category: String) {
        view?.showLoading()
        executor.execute {
            try {
                val cocktails = if (currentSearchQuery.isEmpty()) {
                    cocktailRepository.filterByCategory(category)
                } else {
                    val searchResults = cocktailRepository.searchCocktails(currentSearchQuery)
                    searchResults.filter { cocktail ->
                        cocktail.strCategory?.equals(category, ignoreCase = true) == true
                    }
                }

                paginationData.setData(cocktails)

                view?.let { v ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        v.hideLoading()
                        v.showCocktails(paginationData.currentPageItems)
                        updatePaginationUI()
                    }
                }
            } catch (e: Exception) {
                view?.let { v ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        v.hideLoading()
                        v.showError("Error filtering cocktails: ${e.message}")
                    }
                }
            }
        }
    }

    override fun filterByAlcoholic(alcoholic: String) {
        view?.showLoading()
        executor.execute {
            try {
                val cocktails = if (currentSearchQuery.isEmpty()) {
                    cocktailRepository.filterByAlcoholic(alcoholic)
                } else {
                    val searchResults = cocktailRepository.searchCocktails(currentSearchQuery)
                    searchResults.filter { cocktail ->
                        cocktail.strAlcoholic?.equals(alcoholic, ignoreCase = true) == true
                    }
                }

                paginationData.setData(cocktails)

                view?.let { v ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        v.hideLoading()
                        v.showCocktails(paginationData.currentPageItems)
                        updatePaginationUI()
                    }
                }
            } catch (e: Exception) {
                view?.let { v ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        v.hideLoading()
                        v.showError("Error filtering cocktails: ${e.message}")
                    }
                }
            }
        }
    }

    override fun loadCategories() {
        executor.execute {
            try {
                val categories = cocktailRepository.getCategories()

                view?.let { v ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        // TODO: Update categories UI if needed
                    }
                }
            } catch (e: Exception) {
                view?.let { v ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        v.showError("Error loading categories: ${e.message}")
                    }
                }
            }
        }
    }

    override fun nextPage() {
        if (paginationData.nextPage()) {
            view?.showCocktails(paginationData.currentPageItems)
            updatePaginationUI()
        }
    }

    override fun previousPage() {
        if (paginationData.previousPage()) {
            view?.showCocktails(paginationData.currentPageItems)
            updatePaginationUI()
        }
    }

    override fun goToPage(page: Int) {
        if (paginationData.goToPage(page)) {
            view?.showCocktails(paginationData.currentPageItems)
            updatePaginationUI()
        }
    }

    override fun onCocktailClicked(cocktail: Cocktail) {
        view?.navigateToCocktailDetail(cocktail)
    }

    private fun updatePaginationUI() {
        view?.updatePagination(
            currentPage = paginationData.getCurrentPage(),
            totalPages = paginationData.totalPages,
            hasNext = paginationData.hasNextPage,
            hasPrevious = paginationData.hasPreviousPage
        )
        view?.showPagination(paginationData.getTotalItems() > 10)
    }
}

