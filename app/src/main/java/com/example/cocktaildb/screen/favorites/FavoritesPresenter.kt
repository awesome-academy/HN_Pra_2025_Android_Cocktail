package com.example.cocktaildb.screen.favorites

import com.example.cocktaildb.data.manager.FavoritesManager
import com.example.cocktaildb.data.model.Cocktail

class FavoritesPresenter : FavoritesContract.Presenter {

    private var view: FavoritesContract.View? = null

    override fun setView(view: FavoritesContract.View?) {
        this.view = view
        // Load favorites when view is set
        if (view != null) {
            loadFavorites()
        }
    }

    override fun onStart() {
        // Called when the presenter starts
    }

    override fun onStop() {
        // Called when the presenter stops
    }

    override fun loadFavorites() {
        view?.displayLoading(true)

        // Load favorites from FavoritesManager
        FavoritesManager.loadFavoritesFromFirestore { success ->
            if (success) {
                // Get the cached favorites
                val favorites = FavoritesManager.getFavorites()

                if (favorites.isNotEmpty()) {
                    view?.displayLoading(false)
                    view?.displayFavorites(favorites)
                } else {
                    view?.displayLoading(false)
                    view?.displayEmptyState()
                }
            } else {
                view?.displayLoading(false)
                view?.displayEmptyState()
            }
        }
    }
}
