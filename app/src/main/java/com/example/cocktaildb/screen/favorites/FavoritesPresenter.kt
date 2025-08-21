package com.example.cocktaildb.screen.favorites

import android.content.Context
import android.util.Log
import com.example.cocktaildb.data.manager.FavoritesManager
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.Favorite
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.FavoriteFirebaseService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class FavoritesPresenter(
    private val context: Context,
    private val cocktailRepository: CocktailRepository
) : FavoritesContract.Presenter {

    private var view: FavoritesContract.View? = null
    private val favoriteFirebaseService = FavoriteFirebaseService()
    private val auth = FirebaseAuth.getInstance()
    private var presenterJob: Job? = null
    private val TAG = "FavoritesPresenter"

    private var cachedFavorites: List<Cocktail>? = null
    private var isLoading = false

    override fun setView(view: FavoritesContract.View?) {
        this.view = view
        // Preload offline favorites into memory for quick access via repository
        cocktailRepository.preloadOfflineFavorites(context)
        // Show cached data only if we're not currently loading
        if (!isLoading) {
            cachedFavorites?.let { favorites ->
                view?.displayFavorites(favorites)
            }
        }
    }

    override fun onStart() {
        if (cachedFavorites == null) {
            loadFavorites()
        }
    }

    override fun onStop() {
        presenterJob?.cancel()
        presenterJob = null
        view = null
    }

    override fun loadFavorites() {
        if (isLoading) return
        isLoading = true
        view?.displayLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "loadFavorites: No current user found, falling back to offline favorites")
            displayOfflineFavorites()
            isLoading = false
            return
        }

        Log.d(TAG, "loadFavorites: Loading favorites for user ${currentUser.uid}")
        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val favoritesResult = withContext(Dispatchers.IO) {
                    favoriteFirebaseService.getUserFavorites(currentUser.uid)
                }

                if (favoritesResult.isSuccess) {
                    val favorites: List<Favorite> = favoritesResult.getOrNull() ?: emptyList()
                    val cocktails = withContext(Dispatchers.IO) {
                        favorites.mapNotNull { favorite ->
                            cocktailRepository.getCocktailById(favorite.cocktailId)
                        }
                    }
                    cachedFavorites = cocktails
                    view?.displayLoading(false)
                    if (cocktails.isEmpty()) view?.displayEmptyState() else view?.displayFavorites(cocktails)
                } else {
                    throw favoritesResult.exceptionOrNull() ?: Exception("Failed to load favorites")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading favorites, showing offline cache", e)
                view?.displayLoading(false)
                displayOfflineFavorites()
            } finally {
                isLoading = false
            }
        }
    }

    private fun displayOfflineFavorites() {
        val offline = cocktailRepository.getOfflineFavorites(context)
        cachedFavorites = offline
        if (offline.isEmpty()) {
            view?.displayEmptyState()
        } else {
            view?.displayFavorites(offline)
        }
    }

    override fun addToFavorites(cocktail: Cocktail) {
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            FavoritesManager.toggleFavoriteOfflineAware(context, cocktail) { isFavorite ->
                if (isFavorite) {
                    view?.showFavoriteAdded(cocktail)
                }
                loadFavorites()
            }
        }
    }

    override fun removeFromFavorites(cocktail: Cocktail) {
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            FavoritesManager.toggleFavoriteOfflineAware(context, cocktail) { isFavorite ->
                if (!isFavorite) {
                    view?.showFavoriteRemoved(cocktail)
                }
                loadFavorites()
            }
        }
    }

    override fun toggleFavorite(cocktail: Cocktail) {
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            FavoritesManager.toggleFavoriteOfflineAware(context, cocktail) {
                loadFavorites()
            }
        }
    }
}
