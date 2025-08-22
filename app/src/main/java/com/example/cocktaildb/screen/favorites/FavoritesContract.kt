package com.example.cocktaildb.screen.favorites

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface FavoritesContract {

    interface View {
        fun displayLoading(show: Boolean)
        fun displayFavorites(favorites: List<Cocktail>)
        fun displayEmptyState()
        fun displayError(message: String)
        fun showFavoriteAdded(cocktail: Cocktail)
        fun showFavoriteRemoved(cocktail: Cocktail)
        fun showSyncStatus(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadFavorites()
        fun addToFavorites(cocktail: Cocktail)
        fun removeFromFavorites(cocktail: Cocktail)
        fun toggleFavorite(cocktail: Cocktail)
        fun syncFavoritesIfNeeded()
        fun clearAllFavorites()
    }
}
