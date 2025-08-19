package com.example.cocktaildb.screen.favorites

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.FavoriteFirebaseService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class FavoritesPresenter : FavoritesContract.Presenter {

    private var view: FavoritesContract.View? = null
    private val favoriteFirebaseService = FavoriteFirebaseService()
    private val cocktailRepository = CocktailRepository()
    private val auth = FirebaseAuth.getInstance()
    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())
    private val TAG = "FavoritesPresenter"

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
        presenterScope.cancel() // Cancel all coroutines when stopping
        view = null
    }

    override fun loadFavorites() {
        view?.displayLoading(true)

        // Get current user
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "loadFavorites: No current user found")
            view?.displayLoading(false)
            view?.displayEmptyState()
            return
        }

        Log.d(TAG, "loadFavorites: Loading favorites for user ${currentUser.uid}")

        presenterScope.launch {
            try {
                // Get user's favorites from Firebase
                Log.d(TAG, "loadFavorites: Fetching favorites from Firebase")
                val favoritesResult = withContext(Dispatchers.IO) {
                    try {
                        favoriteFirebaseService.getUserFavorites(currentUser.uid)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching favorites: ${e.message}", e)
                        Result.failure(e)
                    }
                }

                if (favoritesResult.isSuccess) {
                    val favorites = favoritesResult.getOrNull() ?: emptyList()
                    Log.d(TAG, "loadFavorites: Found ${favorites.size} favorites")

                    if (favorites.isNotEmpty()) {
                        // Fetch all cocktail details in parallel using async/await
                        val deferredCocktails = favorites.map { favorite ->
                            async(Dispatchers.IO) {
                                Log.d(TAG, "loadFavorites: Fetching cocktail details for ID: ${favorite.cocktailId}")
                                cocktailRepository.getCocktailById(favorite.cocktailId)
                            }
                        }

                        // Wait for all requests to complete
                        val cocktails = deferredCocktails.awaitAll().filterNotNull()

                        Log.d(TAG, "loadFavorites: Successfully fetched ${cocktails.size} cocktails")
                        if (cocktails.isNotEmpty()) {
                            view?.displayLoading(false)
                            view?.displayFavorites(cocktails)
                        } else {
                            Log.d(TAG, "loadFavorites: No cocktails found for favorites, showing empty state")
                            view?.displayLoading(false)
                            view?.displayEmptyState()
                        }
                    } else {
                        Log.d(TAG, "loadFavorites: No favorites found, showing empty state")
                        view?.displayLoading(false)
                        view?.displayEmptyState()
                    }
                } else {
                    val exception = favoritesResult.exceptionOrNull()
                    Log.e(TAG, "Failed to load favorites: ${exception?.message}", exception)
                    view?.displayLoading(false)
                    view?.displayError("Failed to load favorites: ${exception?.message ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadFavorites: ${e.message}", e)
                view?.displayLoading(false)
                view?.displayError("An error occurred: ${e.message}")
            }
        }
    }

    override fun addToFavorites(cocktail: Cocktail) {
        val currentUser = auth.currentUser ?: return

        presenterScope.launch {
            try {
                // Add to favorites directly without checking or saving cocktail
                val result = withContext(Dispatchers.IO) {
                    favoriteFirebaseService.addFavorite(currentUser.uid, cocktail.idDrink)
                }

                if (result.isSuccess) {
                    view?.showFavoriteAdded(cocktail)
                    // Reload favorites to update the UI
                    loadFavorites()
                } else {
                    view?.displayError("Failed to add to favorites")
                }
            } catch (e: Exception) {
                view?.displayError("Error adding to favorites: ${e.message}")
            }
        }
    }

    override fun removeFromFavorites(cocktail: Cocktail) {
        val currentUser = auth.currentUser ?: return

        presenterScope.launch {
            try {
                // Get the favorite document
                val favoriteResult = withContext(Dispatchers.IO) {
                    favoriteFirebaseService.getFavorite(currentUser.uid, cocktail.idDrink)
                }

                if (favoriteResult.isSuccess && favoriteResult.getOrNull() != null) {
                    // Remove from favorites
                    val result = withContext(Dispatchers.IO) {
                        favoriteFirebaseService.removeFavorite(currentUser.uid, cocktail.idDrink)
                    }

                    if (result.isSuccess && result.getOrNull() == true) {
                        view?.showFavoriteRemoved(cocktail)
                        // Reload favorites to update the UI
                        loadFavorites()
                    } else {
                        view?.displayError("Failed to remove from favorites")
                    }
                } else {
                    view?.displayError("Cocktail not found in favorites")
                }
            } catch (e: Exception) {
                view?.displayError("Error removing from favorites: ${e.message}")
            }
        }
    }

    override fun toggleFavorite(cocktail: Cocktail) {
        val currentUser = auth.currentUser ?: return

        presenterScope.launch {
            try {
                // Check if the cocktail is already a favorite
                val isFavorite = withContext(Dispatchers.IO) {
                    val result = favoriteFirebaseService.isFavorite(currentUser.uid, cocktail.idDrink)
                    result.isSuccess && result.getOrNull() == true
                }

                if (isFavorite) {
                    removeFromFavorites(cocktail)
                } else {
                    addToFavorites(cocktail)
                }
            } catch (e: Exception) {
                view?.displayError("Error toggling favorite: ${e.message}")
            }
        }
    }
}
