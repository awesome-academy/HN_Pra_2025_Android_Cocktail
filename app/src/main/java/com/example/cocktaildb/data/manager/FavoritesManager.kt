package com.example.cocktaildb.data.manager

import android.content.Context
import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.Favorite
import com.example.cocktaildb.utils.ImageLoader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Manages favorite cocktails in memory, supports offline cache, and syncs with Firestore
 */
object FavoritesManager {
    private val favoriteCocktails = mutableSetOf<Cocktail>()
    private val favoritesById = mutableMapOf<String, String>() // cocktailId -> favoriteId
    private var isInitialized = false
    private const val TAG = "FavoritesManager"

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private const val FAVORITES_COLLECTION = "favorites"

    // Offline storage
    private const val OFFLINE_PREFS = "cocktail_favorites"
    private const val OFFLINE_KEY = "favorites"

    // Check if manager is initialized with user favorites
    fun isInitialized(): Boolean = isInitialized

    // Load all favorites for the current user from Firestore
    fun loadFavoritesFromFirestore(onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false)
            return
        }

        firestore.collection(FAVORITES_COLLECTION)
            .whereEqualTo("uid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                // Clear existing favorites
                favoriteCocktails.clear()
                favoritesById.clear()

                for (document in documents) {
                    val favorite = document.toObject(Favorite::class.java)
                    favoritesById[favorite.cocktailId] = favorite.id
                }

                isInitialized = true
                Log.d(TAG, "Loaded ${favoritesById.size} favorites for user ${currentUser.uid}")
                onComplete(true)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to load favorites: ${it.message}")
                onComplete(false)
            }
    }

    // Load offline favorites into memory (does not change isInitialized)
    fun preloadOfflineFavorites(context: Context) {
        val list = getOfflineFavorites(context)
        favoriteCocktails.clear()
        favoriteCocktails.addAll(list)
    }

    // Get offline favorites
    fun getOfflineFavorites(context: Context): List<Cocktail> {
        return try {
            val prefs = context.getSharedPreferences(OFFLINE_PREFS, Context.MODE_PRIVATE)
            val dataString = prefs.getString(OFFLINE_KEY, "") ?: ""
            if (dataString.isEmpty()) emptyList() else dataString.split("||").mapNotNull { entry ->
                val fields = entry.split("|:|")
                if (fields.size >= 3) {
                    Cocktail(idDrink = fields[0], strDrink = fields[1], strDrinkThumb = fields[2])
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveOfflineFavorites(context: Context, items: List<Cocktail>) {
        runCatching {
            val prefs = context.getSharedPreferences(OFFLINE_PREFS, Context.MODE_PRIVATE)
            val dataString = items.joinToString("||") { "${it.idDrink}|:|${it.strDrink}|:|${it.strDrinkThumb ?: ""}" }
            prefs.edit().putString(OFFLINE_KEY, dataString).apply()
        }
    }

    private fun ensureLocalThumb(context: Context, cocktail: Cocktail): Cocktail {
        val url = cocktail.strDrinkThumb
        return if (!url.isNullOrEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
            val path = ImageLoader.saveImageFromUrlToInternalStorage(context, url)
            if (path != null) cocktail.copy(strDrinkThumb = ImageLoader.getFileUri(path)) else cocktail
        } else cocktail
    }

    // Add a favorite to Firestore (prevents duplicates)
    fun addFavoriteToFirestore(cocktail: Cocktail, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false)
            return
        }

        // Check if the cocktail is already a favorite
        if (isFavorite(cocktail.idDrink)) {
            Log.d(TAG, "Cocktail ${cocktail.idDrink} is already a favorite, skipping add")
            onComplete(true)
            return
        }

        val favoriteId = UUID.randomUUID().toString()
        val favorite = Favorite(
            id = favoriteId,
            cocktailId = cocktail.idDrink,
            uid = currentUser.uid,
            createdAt = System.currentTimeMillis()
        )

        firestore.collection(FAVORITES_COLLECTION)
            .document(favoriteId)
            .set(favorite)
            .addOnSuccessListener {
                favoriteCocktails.add(cocktail)
                favoritesById[cocktail.idDrink] = favoriteId
                Log.d(TAG, "Added cocktail ${cocktail.idDrink} to favorites")
                onComplete(true)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to add favorite: ${it.message}")
                onComplete(false)
            }
    }

    // Remove a favorite from Firestore
    fun removeFavoriteFromFirestore(cocktail: Cocktail, onComplete: (Boolean) -> Unit) {
        val favoriteId = favoritesById[cocktail.idDrink] ?: return onComplete(false)

        firestore.collection(FAVORITES_COLLECTION)
            .document(favoriteId)
            .delete()
            .addOnSuccessListener {
                favoriteCocktails.remove(cocktail)
                favoritesById.remove(cocktail.idDrink)
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    // In-memory operations
    fun addFavorite(cocktail: Cocktail) { favoriteCocktails.add(cocktail) }
    fun removeFavorite(cocktail: Cocktail) { favoriteCocktails.remove(cocktail) }

    fun isFavorite(cocktailId: String): Boolean {
        return favoritesById.containsKey(cocktailId) || favoriteCocktails.any { it.idDrink == cocktailId }
    }

    fun getFavorites(): List<Cocktail> = favoriteCocktails.toList()

    // Offline operations
    fun addFavoriteOffline(context: Context, cocktail: Cocktail): Boolean {
        val items = getOfflineFavorites(context).toMutableList()
        items.removeAll { it.idDrink == cocktail.idDrink }
        val updated = ensureLocalThumb(context, cocktail)
        items.add(0, updated)
        saveOfflineFavorites(context, items)
        favoriteCocktails.add(updated)
        return true
    }

    fun removeFavoriteOffline(context: Context, cocktail: Cocktail): Boolean {
        val items = getOfflineFavorites(context).toMutableList()
        val removed = items.removeAll { it.idDrink == cocktail.idDrink }

        Log.d("FavoritesManager", "Trying to remove offline: ${cocktail.strDrink} (id=${cocktail.idDrink})")
        Log.d("FavoritesManager", "Removed from local list? $removed (before size=${items.size + if (removed) 1 else 0}, after size=${items.size})")
        Log.d("FavoritesManager", "Before saving, items = ${items.map { it.strDrink }}")
        saveOfflineFavorites(context, items)
        val check = getOfflineFavorites(context)
        Log.d("FavoritesManager", "After saving, offline favorites = ${check.map { it.strDrink }}")
        val removedFromCache = favoriteCocktails.removeIf { it.idDrink == cocktail.idDrink }
        Log.d("FavoritesManager", "Removed from in-memory cache? $removedFromCache")

        return removed
    }

    // Toggle favorite with Firestore if possible, otherwise fallback to offline
    fun toggleFavoriteOfflineAware(context: Context, cocktail: Cocktail, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val newState = if (isFavorite(cocktail.idDrink)) {
                removeFavoriteOffline(context, cocktail)
                false
            } else {
                addFavoriteOffline(context, cocktail)
                true
            }
            onComplete(newState)
            return
        }

        if (isFavorite(cocktail.idDrink)) {
            removeFavoriteFromFirestore(cocktail) { success ->
                if (success) {
                    removeFavoriteOffline(context, cocktail) // keep offline in sync
                    onComplete(false)
                } else {
                    // Fallback offline
                    val removed = removeFavoriteOffline(context, cocktail)
                    onComplete(!removed)
                }
            }
        } else {
            // Ensure local thumb even when online so it's available offline later
            val updated = ensureLocalThumb(context, cocktail)
            addFavoriteToFirestore(updated) { success ->
                if (success) {
                    addFavoriteOffline(context, updated) // mirror offline
                    onComplete(true)
                } else {
                    // Fallback offline
                    addFavoriteOffline(context, updated)
                    onComplete(true)
                }
            }
        }
    }

    // Backward-compatible toggles
    fun toggleFavorite(cocktail: Cocktail, onComplete: (Boolean) -> Unit) {
        if (isFavorite(cocktail.idDrink)) {
            removeFavoriteFromFirestore(cocktail) { success ->
                if (success) removeFavorite(cocktail)
                onComplete(success)
            }
        } else {
            addFavoriteToFirestore(cocktail) { success ->
                if (success) addFavorite(cocktail)
                onComplete(success)
            }
        }
    }

    fun toggleFavorite(cocktail: Cocktail): Boolean {
        val isFav = isFavorite(cocktail.idDrink)
        if (isFav) {
            removeFavoriteFromFirestore(cocktail) { success ->
                if (!success) { addFavorite(cocktail); Log.e(TAG, "Failed to remove favorite from Firestore, reverting local state") }
            }
            removeFavorite(cocktail)
        } else {
            addFavoriteToFirestore(cocktail) { success ->
                if (!success) { removeFavorite(cocktail); Log.e(TAG, "Failed to add favorite to Firestore, reverting local state") }
            }
            addFavorite(cocktail)
        }
        return !isFav
    }
}
