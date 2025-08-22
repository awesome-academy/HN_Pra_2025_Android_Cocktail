package com.example.cocktaildb.data.service

import android.util.Log
import com.example.cocktaildb.data.model.History
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.CocktailDetail
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class HistoryFirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val historyCollection = firestore.collection("history")

    companion object {
        private const val TAG = "HistoryFirebaseService"
    }
    suspend fun addHistoryWithDetails(uid: String, cocktail: Cocktail): Result<String> {
        return try {
            Log.d(TAG, "addHistoryWithDetails called: uid=$uid, cocktail=${cocktail.strDrink}")
            val historyId = UUID.randomUUID().toString()
            val cocktailDetail = CocktailDetail.fromCocktail(cocktail)
            val history = History(
                id = historyId,
                uid = uid,
                cocktailId = cocktail.idDrink,
                cocktailDetail = cocktailDetail
            )
            
            Log.d(TAG, "Created detailed history object with embedded cocktail detail: ${cocktail.strDrink}")
            Log.d(TAG, "Attempting to save detailed history to Firestore...")
            
            historyCollection.document(historyId).set(history).await()
            
            Log.d(TAG, "Successfully saved detailed history to Firestore with ID: $historyId")
            Result.success(historyId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving detailed history to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun addHistory(uid: String, cocktailId: String): Result<String> {
        return try {
            val historyId = UUID.randomUUID().toString()
            val history = History(
                id = historyId,
                uid = uid,
                cocktailId = cocktailId
            )
            
            Log.d(TAG, "Created history object: $history")
            Log.d(TAG, "Collection path: ${historyCollection.path}")
            Log.d(TAG, "Document path: ${historyCollection.document(historyId).path}")
            Log.d(TAG, "Attempting to save to Firestore...")
            
            historyCollection.document(historyId).set(history).await()
            
            Log.d(TAG, "Successfully saved history to Firestore with ID: $historyId")
            Result.success(historyId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add history: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserHistory(uid: String): Result<List<History>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .get().await()
            val history = querySnapshot.toObjects(History::class.java)
                .sortedByDescending { it.viewedAt }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserHistoryPaginated(uid: String, limit: Int = 50): Result<List<History>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .get().await()
            val history = querySnapshot.toObjects(History::class.java)
                .sortedByDescending { it.viewedAt }
                .take(limit)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentHistory(uid: String, limit: Int = 10): Result<List<History>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .get().await()
            val history = querySnapshot.toObjects(History::class.java)
                .sortedByDescending { it.viewedAt }
                .take(limit)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCocktailViewHistory(cocktailId: String): Result<List<History>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            val history = querySnapshot.toObjects(History::class.java)
                .sortedByDescending { it.viewedAt }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getViewCount(cocktailId: String): Result<Int> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            Result.success(querySnapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeHistoryItem(historyId: String): Result<Boolean> {
        return try {
            historyCollection.document(historyId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearUserHistory(uid: String): Result<Boolean> {
        return try {
            val querySnapshot = historyCollection
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

    suspend fun removeOldHistory(uid: String, daysToKeep: Int = 30): Result<Boolean> {
        return try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)

            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .whereLessThan("viewedAt", cutoffTime)
                .get().await()

            for (document in querySnapshot.documents) {
                document.reference.delete().await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isInHistory(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get().await()
            Result.success(!querySnapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUniqueHistoryCocktails(uid: String): Result<List<String>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .get().await()

            val uniqueCocktailIds = mutableSetOf<String>()
            for (document in querySnapshot.documents) {
                val cocktailId = document.getString("cocktailId")
                if (cocktailId != null) {
                    uniqueCocktailIds.add(cocktailId)
                }
            }

            val historyList = querySnapshot.toObjects(History::class.java)
                .sortedByDescending { it.viewedAt }
            
            val sortedUniqueIds = mutableListOf<String>()
            for (history in historyList) {
                if (!sortedUniqueIds.contains(history.cocktailId)) {
                    sortedUniqueIds.add(history.cocktailId)
                }
            }

            Result.success(sortedUniqueIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMostViewedCocktails(limit: Int = 20): Result<Map<String, Int>> {
        return try {
            val cocktailViewsCollection = firestore.collection("cocktailViews")
            val querySnapshot = cocktailViewsCollection
                .limit(limit.toLong())
                .get().await()
            val cocktailViewCounts = mutableMapOf<String, Int>()
            for (document in querySnapshot.documents) {
                val cocktailId = document.getString("cocktailId")
                val viewCount = document.getLong("viewCount")?.toInt() ?: 0
                if (cocktailId != null) {
                    cocktailViewCounts[cocktailId] = viewCount
                }
            }
            val sortedEntries = cocktailViewCounts.entries.sortedByDescending { it.value }
            Result.success(sortedEntries.associate { it.key to it.value })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
