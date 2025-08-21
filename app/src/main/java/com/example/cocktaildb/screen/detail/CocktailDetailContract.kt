package com.example.cocktaildb.screen.detail

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface CocktailDetailContract {
    interface View {
        fun showRelatedCocktails(cocktails: List<Cocktail>)
        fun showError(message: String)
        fun updateBookmarkButtonState(isBookmarked: Boolean)
    }

    interface Presenter : BasePresenter<View> {
        fun loadRelatedCocktails(cocktailName: String, category: String)
        override fun setView(view: View?)
        fun checkBookmarkStatus(cocktailId: String)
        fun toggleBookmark(cocktail: Cocktail)
        override fun onStart()
        override fun onStop()
    }
}
