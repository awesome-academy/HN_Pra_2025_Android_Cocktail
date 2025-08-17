package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.History
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class HistoryFirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val historyCollection = firestore.collection("history")

    suspend fun addHistory(uid: String, cocktailId: String): Result<String> {
        return try {
            val historyId = UUID.randomUUID().toString()
            val history = History(
                id = historyId,
                uid = uid,
                cocktailId = cocktailId
            )
            historyCollection.document(historyId).set(history).await()
            Result.success(historyId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserHistory(uid: String): Result<List<History>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .get().await()
            val history = querySnapshot.toObjects(History::class.java)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserHistoryPaginated(uid: String, limit: Int = 50): Result<List<History>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
            val history = querySnapshot.toObjects(History::class.java)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentHistory(uid: String, limit: Int = 10): Result<List<History>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("uid", uid)
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
            val history = querySnapshot.toObjects(History::class.java)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCocktailViewHistory(cocktailId: String): Result<List<History>> {
        return try {
            val querySnapshot = historyCollection
                .whereEqualTo("cocktailId", cocktailId)
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .get().await()
            val history = querySnapshot.toObjects(History::class.java)
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
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .get().await()

            val uniqueCocktailIds = mutableSetOf<String>()
            for (document in querySnapshot.documents) {
                val cocktailId = document.getString("cocktailId")
                if (cocktailId != null) {
                    uniqueCocktailIds.add(cocktailId)
                }
            }

            Result.success(uniqueCocktailIds.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMostViewedCocktails(limit: Int = 20): Result<Map<String, Int>> {
        return try {
            val cocktailViewsCollection = firestore.collection("cocktailViews")
            val querySnapshot = cocktailViewsCollection
                .orderBy("viewCount", Query.Direction.DESCENDING)
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
            Result.success(cocktailViewCounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
