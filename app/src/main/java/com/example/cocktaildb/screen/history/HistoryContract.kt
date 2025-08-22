package com.example.cocktaildb.screen.history

import com.example.cocktaildb.data.model.Cocktail

interface HistoryContract {
    interface View {
        fun showHistoryCocktails(cocktails: List<Cocktail>)
        fun showEmptyState()
        fun hideEmptyState()
        fun displayLoading(show: Boolean)
        fun displayError(message: String)
        fun navigateToCocktailDetail(cocktail: Cocktail)
        fun showSyncStatus(message: String)
    }

    interface Presenter {
        fun setView(view: View?)
        fun onStart()
        fun onStop()
        fun loadHistoryCocktails()
        fun onCocktailClicked(cocktail: Cocktail)
        fun clearHistory()
    }
} 