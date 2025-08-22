package com.example.cocktaildb.screen.allcocktails

import com.example.cocktaildb.data.model.Cocktail

interface AllCocktailsContract {

    interface View {
        fun showCocktails(cocktails: List<Cocktail>)
        fun showLoadingState()
        fun hideLoadingState()
        fun showErrorMessage(message: String)
        fun showMessage(message: String)
        fun updatePagination(currentPage: Int, totalPages: Int, hasNext: Boolean, hasPrevious: Boolean)
        fun showPagination(show: Boolean)
        fun navigateToCocktailDetail(cocktail: Cocktail)
        fun showCocktailDetail(cocktail: Cocktail)
        fun showSearchResults()
    }

    interface Presenter {
        fun setView(view: View?)
        fun onStart()
        fun onStop()
        fun loadAllCocktails()
        fun loadMoreCocktails()
        fun searchCocktails(query: String)
        fun filterByCategory(category: String)
        fun filterByAlcoholic(alcoholicType: String)
        fun onCocktailClicked(cocktail: Cocktail)
        fun nextPage()
        fun previousPage()
        fun goToPage(page: Int)
    }
} 


