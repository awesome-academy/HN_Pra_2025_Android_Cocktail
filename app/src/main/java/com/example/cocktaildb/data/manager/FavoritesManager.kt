package com.example.cocktaildb.data.manager

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.Favorite
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Manages favorite cocktails in memory and syncs with Firestore
 */
object FavoritesManager {
    private val favoriteCocktails = mutableSetOf<Cocktail>()
    private val favoritesById = mutableMapOf<String, String>() // cocktailId -> favoriteId
    private var isInitialized = false
    private const val TAG = "FavoritesManager"

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private const val FAVORITES_COLLECTION = "favorites"

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

    // In-memory operations (synced with Firestore)
    fun addFavorite(cocktail: Cocktail) {
        favoriteCocktails.add(cocktail)
    }

    fun removeFavorite(cocktail: Cocktail) {
        favoriteCocktails.remove(cocktail)
    }

    fun isFavorite(cocktailId: String): Boolean {
        return favoritesById.containsKey(cocktailId) ||
               favoriteCocktails.any { it.idDrink == cocktailId }
    }

    fun getFavorites(): List<Cocktail> {
        return favoriteCocktails.toList()
    }

    // Toggle favorite status with Firestore sync
    fun toggleFavorite(cocktail: Cocktail, onComplete: (Boolean) -> Unit) {
        if (isFavorite(cocktail.idDrink)) {
            removeFavoriteFromFirestore(cocktail) { success ->
                if (success) {
                    removeFavorite(cocktail)
                }
                onComplete(success)
            }
        } else {
            addFavoriteToFirestore(cocktail) { success ->
                if (success) {
                    addFavorite(cocktail)
                }
                onComplete(success)
            }
        }
    }

    // Toggle favorite status (simplified for immediate UI response)
    fun toggleFavorite(cocktail: Cocktail): Boolean {
        val isFavorite = isFavorite(cocktail.idDrink)
        if (isFavorite) {
            removeFavoriteFromFirestore(cocktail) { success ->
                if (!success) {
                    // Revert local state if Firestore operation failed
                    addFavorite(cocktail)
                    Log.e(TAG, "Failed to remove favorite from Firestore, reverting local state")
                }
            }
            removeFavorite(cocktail)
        } else {
            addFavoriteToFirestore(cocktail) { success ->
                if (!success) {
                    // Revert local state if Firestore operation failed
                    removeFavorite(cocktail)
                    Log.e(TAG, "Failed to add favorite to Firestore, reverting local state")
                }
            }
            addFavorite(cocktail)
        }
        return !isFavorite
    }
}
