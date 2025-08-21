package com.example.cocktaildb.screen.search

import com.example.cocktaildb.data.model.Cocktail

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
    }

    interface Presenter {
        fun setView(view: View?)
        fun onStart()
        fun onStop()
        fun searchCocktails(query: String)
        fun filterByCategory(category: String)
        fun filterByAlcoholic(alcoholic: String)
        fun loadCategories()
        fun nextPage()
        fun previousPage()
        fun goToPage(page: Int)
        fun onCocktailClicked(cocktail: Cocktail)
        fun addToHistory(cocktail: Cocktail)
    }
}

