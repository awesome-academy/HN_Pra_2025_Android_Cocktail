package com.example.cocktaildb.screen.favorites

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface FavoritesContract {

    interface View {
        fun displayLoading(show: Boolean)
        fun displayFavorites(favorites: List<Cocktail>)
        fun displayEmptyState()
        fun displayError(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadFavorites()
    }
}
