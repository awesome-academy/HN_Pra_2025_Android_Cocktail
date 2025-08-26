package com.example.cocktaildb.screen.search

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface SearchContract {
    interface View {
        fun showLoading()
        fun hideLoading()
        fun showCocktails(cocktails: List<Cocktail>)
        fun showError(message: String)
        fun showMessage(message: String)
        fun updatePagination(currentPage: Int, totalPages: Int, hasNext: Boolean, hasPrevious: Boolean)
        fun showPagination(show: Boolean)
        fun navigateToCocktailDetail(cocktail: Cocktail)
        
        // Search suggestions
        fun showSearchSuggestions(suggestions: List<String>)
        fun hideSearchSuggestions()
        fun updateSearchText(query: String)
        
        // Empty state
        fun showEmptyState(message: String)
        fun hideEmptyState()
    }

    interface Presenter : BasePresenter<View> {
        fun searchCocktails(query: String)
        fun filterByCategory(category: String)
        fun filterByAlcoholic(alcoholic: String)
        fun loadCategories()
        fun nextPage()
        fun previousPage()
        fun goToPage(page: Int)
        fun onCocktailClicked(cocktail: Cocktail)
        
        // Search suggestions
        fun onSearchTextChanged(query: String)
        fun onSearchFocused()
        fun onSearchSubmitted(query: String)
        fun onSuggestionClicked(suggestion: String)
        fun onSuggestionRemoved(suggestion: String)
        fun loadRecentSearches()
    }
}

