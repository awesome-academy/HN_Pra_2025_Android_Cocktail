package com.example.cocktaildb.screen.detail

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface CocktailDetailContract {
    interface View {
        fun showRelatedCocktails(cocktails: List<Cocktail>)
        fun showError(message: String)
        fun updateBookmarkButtonState(isBookmarked: Boolean)
        fun updateFavoriteButtonState(isFavorite: Boolean)
        fun showMessage(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadRelatedCocktails(cocktailName: String, category: String)
        fun checkBookmarkStatus(cocktailId: String)
        fun toggleBookmark(cocktail: Cocktail)
        fun checkFavoriteStatus(cocktailId: String)
        fun toggleFavorite(cocktail: Cocktail)
        fun addToHistory(cocktail: Cocktail)
        override fun setView(view: View?)
        override fun onStart()
        override fun onStop()
    }
}
