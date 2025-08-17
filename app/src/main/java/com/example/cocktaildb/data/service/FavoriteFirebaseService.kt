package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.Favorite
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FavoriteFirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val favoritesCollection = firestore.collection("favorites")

    suspend fun addFavorite(uid: String, cocktailId: String): Result<String> {
        return try {
            val favoriteId = UUID.randomUUID().toString()
            val favorite = Favorite(
                id = favoriteId,
                uid = uid,
                cocktailId = cocktailId
            )
            favoritesCollection.document(favoriteId).set(favorite).await()
            Result.success(favoriteId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFavorites(uid: String): Result<List<Favorite>> {
        return try {
            val querySnapshot = favoritesCollection
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val favorites = querySnapshot.toObjects(Favorite::class.java)
            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCocktailFavorites(cocktailId: String): Result<List<Favorite>> {
        return try {
            val querySnapshot = favoritesCollection
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            val favorites = querySnapshot.toObjects(Favorite::class.java)
            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavoriteCount(cocktailId: String): Result<Int> {
        return try {
            val querySnapshot = favoritesCollection
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            Result.success(querySnapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFavorite(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            val querySnapshot = favoritesCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            Result.success(!querySnapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavorite(uid: String, cocktailId: String): Result<Favorite?> {
        return try {
            val querySnapshot = favoritesCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()

            if (!querySnapshot.isEmpty) {
                val favorite = querySnapshot.documents[0].toObject(Favorite::class.java)
                Result.success(favorite)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            val querySnapshot = favoritesCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()

            for (document in querySnapshot.documents) {
                document.reference.delete().await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavoriteById(favoriteId: String): Result<Boolean> {
        return try {
            favoritesCollection.document(favoriteId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearUserFavorites(uid: String): Result<Boolean> {
        return try {
            val querySnapshot = favoritesCollection
                .whereEqualTo("uid", uid)
                .get().await()

            for (document in querySnapshot.documents) {
                document.reference.delete().await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
