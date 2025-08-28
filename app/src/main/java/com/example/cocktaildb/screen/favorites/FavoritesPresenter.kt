package com.example.cocktaildb.screen.favorites

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.Favorite
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.manager.FavoritesManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class FavoritesPresenter(
    private val context: Context,
    private val cocktailRepository: CocktailRepository,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : FavoritesContract.Presenter {

    private var view: FavoritesContract.View? = null

    private val presenterJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + presenterJob)

    private val TAG = "FavoritesPresenter"

    private var cachedFavorites: List<Cocktail> = emptyList()
    private var isLoading = false
    private var isProcessingFavorites = false

    companion object {
        private const val MAX_FAVORITES_ITEMS = 100
        private const val FAVORITES_PREFS = "favorites_prefs"
        private const val FAVORITES_KEY = "favorites_list"
        private const val SEPARATOR = "|||"
        private const val FIELD_SEPARATOR = ":::"
    }

    override fun setView(view: FavoritesContract.View?) {
        this.view = view
    }

    override fun onStart() = Unit

    override fun onStop() {
        presenterJob.cancel()
    }

    override fun loadFavorites() {
        if (isLoading) return
        isLoading = true
        view?.displayLoading(true)

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null || !isNetworkAvailable()) {
            Log.d(TAG, "loadFavorites: No user or no network → load local only")
            loadFromLocalOnly()
            return
        }

        Log.d(TAG, "loadFavorites: Sync Firebase ↔ Local for user ${currentUser.uid}")
        syncFirebaseWithLocal(currentUser.uid)
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun syncFirebaseWithLocal(userId: String) {
        uiScope.launch {
            try {

                val firebaseResult = withContext(Dispatchers.IO) {
                    cocktailRepository.getUserFavoritesFromFirebase(userId)
                }

                val firebaseFavorites: List<Favorite> = firebaseResult.getOrElse {
                    Log.e(TAG, "getUserFavoritesFromFirebase failed: ${it.message}", it)
                    emptyList()
                }


                val cocktailsFromFirebase: List<Cocktail> = withContext(Dispatchers.IO) {
                    firebaseFavorites.mapNotNull { fav ->
                        runCatching { cocktailRepository.getCocktailById(fav.cocktailId) }.getOrNull()
                    }
                }


                val localFavorites = withContext(Dispatchers.IO) {
                    cocktailRepository.getFavoritesFromLocal(context)
                }

                if (cocktailsFromFirebase.isEmpty() && localFavorites.isNotEmpty()) {
                    // Trường hợp user vừa online sau khi xài offline → migrate local lên Firebase
                    Log.d(TAG, "Firebase empty & Local has ${localFavorites.size} → migrate up")
                    withContext(Dispatchers.IO) {
                        localFavorites.take(MAX_FAVORITES_ITEMS).forEach { c ->
                            FavoritesManager.addFavoriteToFirestore(c) { /* ignore per-item result */ }
                        }

                        cocktailRepository.saveFavoritesToLocal(context, localFavorites)
                    }
                    cachedFavorites = localFavorites
                    view?.displayFavorites(localFavorites)
                } else {

                    val finalFavorites = deduplicateById(cocktailsFromFirebase)
                    withContext(Dispatchers.IO) {
                        cocktailRepository.saveFavoritesToLocal(context, finalFavorites)
                    }
                    cachedFavorites = finalFavorites
                    if (finalFavorites.isEmpty()) view?.displayEmptyState()
                    else view?.displayFavorites(finalFavorites)
                    Log.d(TAG, "Synced Firebase ${cocktailsFromFirebase.size} → final ${finalFavorites.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing favorites: ${e.message}", e)
                loadFromLocalOnly()
            } finally {
                view?.displayLoading(false)
                isLoading = false
            }
        }
    }

    private fun loadFromLocalOnly() {
        uiScope.launch {
            try {
                val favorites = withContext(Dispatchers.IO) {
                    cocktailRepository.getFavoritesFromLocal(context)
                }
                cachedFavorites = favorites
                if (favorites.isEmpty()) view?.displayEmptyState()
                else view?.displayFavorites(favorites)
            } catch (e: Exception) {
                view?.displayError("Error loading favorites: ${e.message}")
                view?.displayEmptyState()
            } finally {
                view?.displayLoading(false)
                isLoading = false
            }
        }
    }

    private fun deduplicateById(list: List<Cocktail>): List<Cocktail> {
        val seen = HashSet<String>()
        val out = ArrayList<Cocktail>(list.size)
        for (c in list) {
            val id = c.idDrink
            if (seen.add(id)) out.add(c) else Log.d(TAG, "Duplicate removed: ${c.strDrink} ($id)")
        }
        return out
    }

    override fun addToFavorites(cocktail: Cocktail) {
        if (isProcessingFavorites) return
        isProcessingFavorites = true

        uiScope.launch {
            try {

                val ok = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Boolean> { cont ->
                        cocktailRepository.addToFavorites(context, cocktail) { success ->
                            if (cont.isActive) cont.resume(success, onCancellation = null)
                        }
                    }
                }
                if (ok) {
                    view?.showSyncStatus("Added to favorites")
                    loadFavorites()
                } else {
                    view?.displayError("Error adding to favorites")
                }
            } catch (e: Exception) {
                view?.displayError("Error adding to favorites: ${e.message}")
            } finally {
                isProcessingFavorites = false
            }
        }
    }

    override fun removeFromFavorites(cocktail: Cocktail) {
        if (isProcessingFavorites) return
        isProcessingFavorites = true

        uiScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Boolean> { cont ->
                        cocktailRepository.removeFromFavorites(context, cocktail) { success ->
                            if (cont.isActive) cont.resume(success, onCancellation = null)
                        }
                    }
                }
                if (ok) {
                    view?.showSyncStatus("Removed from favorites")
                    loadFavorites()
                } else {
                    view?.displayError("Error removing from favorites")
                }
            } catch (e: Exception) {
                view?.displayError("Error removing from favorites: ${e.message}")
            } finally {
                isProcessingFavorites = false
            }
        }
    }

    override fun toggleFavorite(cocktail: Cocktail) {
        uiScope.launch {
            val state = withContext(Dispatchers.IO) {
                cocktailRepository.toggleFavorite(context, cocktail) // trả về Boolean
            }
            Log.d(TAG, "Favorite toggled → $state")
            loadFavorites()
        }
    }


    override fun clearAllFavorites() {
        uiScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                withContext(Dispatchers.IO) { cocktailRepository.clearAllFavorites(context) }

                if (currentUser != null && isNetworkAvailable()) {
                    val result = withContext(Dispatchers.IO) {
                        cocktailRepository.clearAllFavoritesFromFirebase(currentUser.uid)
                    }
                    if (result.isSuccess) {
                        Log.d(TAG, "Cleared all favorites from Firebase and local")
                        view?.showSyncStatus("All favorites cleared")
                    } else {
                        Log.w(TAG, "Failed to clear Firebase favorites, but local cleared")
                        view?.showSyncStatus("Local favorites cleared")
                    }
                } else {
                    Log.d(TAG, "Cleared local favorites only (no user/offline)")
                    view?.showSyncStatus("Local favorites cleared")
                }
                cachedFavorites = emptyList()
                view?.displayEmptyState()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing favorites", e)
                view?.displayError("Error clearing favorites: ${e.message}")
            }
        }
    }

    override fun syncFavoritesIfNeeded() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null && isNetworkAvailable()) {
            view?.showSyncStatus("Syncing favorites...")
            syncFirebaseWithLocal(currentUser.uid)
        } else {
            view?.showSyncStatus("Offline mode - showing local favorites")
        }
    }
}
