package com.example.cocktaildb.data.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.cocktaildb.data.model.SearchHistory
import com.example.cocktaildb.data.repository.AuthRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class SearchHistoryService(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val authRepository = AuthRepository(context)
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "SearchHistoryService"
        private const val COLLECTION_SEARCH_HISTORY = "search_history"
        private const val PREFS_NAME = "search_history_prefs"
        private const val KEY_SEARCH_HISTORY = "search_history_list"
        private const val MAX_HISTORY_SIZE = 50
        private const val MAX_SUGGESTIONS = 10
    }

    suspend fun addSearchQuery(query: String) {
        try {
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                addSearchQueryToFirebase(currentUser.uid, query)
            } else {
                addSearchQueryToLocal(query)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding search query: $query", e)
        }
    }

    suspend fun getSuggestions(query: String): List<String> {
        return try {
            val currentUser = authRepository.getCurrentUser()
            
            val allSearches = if (currentUser != null) {
                getSearchHistoryFromFirebase(currentUser.uid)
            } else {
                getSearchHistoryFromLocal()
            }

            allSearches
                .filter { it.query.contains(query, ignoreCase = true) && it.query != query }
                .sortedByDescending { it.searchCount }
                .distinctBy { it.query.lowercase() }
                .take(MAX_SUGGESTIONS)
                .map { it.query }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting suggestions for: $query", e)
            emptyList()
        }
    }

    suspend fun getRecentSearches(): List<String> {
        return try {
            val currentUser = authRepository.getCurrentUser()
            
            val recentSearches = if (currentUser != null) {
                getSearchHistoryFromFirebase(currentUser.uid)
            } else {
                getSearchHistoryFromLocal()
            }
            
            recentSearches
                .sortedByDescending { it.lastSearched }
                .take(MAX_SUGGESTIONS)
                .map { it.query }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent searches", e)
            emptyList()
        }
    }

    suspend fun removeSearchQuery(query: String) {
        try {
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                removeSearchQueryFromFirebase(currentUser.uid, query)
            } else {
                removeSearchQueryFromLocal(query)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing search query: $query", e)
        }
    }

    suspend fun clearSearchHistory() {
        try {
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                clearSearchHistoryFromFirebase(currentUser.uid)
            } else {
                clearSearchHistoryFromLocal()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing search history", e)
        }
    }
    
    private suspend fun addSearchQueryToFirebase(userId: String, query: String) {
        try {
            val searchRef = firestore.collection(COLLECTION_SEARCH_HISTORY)
                .document(userId)
                .collection("queries")
                .document(query)
            
            val existingDoc = searchRef.get().await()
            
            if (existingDoc.exists()) {
                searchRef.update(
                    mapOf(
                        "searchCount" to FieldValue.increment(1),
                        "lastSearched" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                val searchHistory = SearchHistory(
                    userId = userId,
                    query = query,
                    searchCount = 1,
                    lastSearched = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                
                searchRef.set(searchHistory.toFirebaseMap()).await()
            }

            cleanupOldFirebaseEntries(userId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding search query to Firebase: $query", e)
            addSearchQueryToLocal(query)
        }
    }
    
    private suspend fun getSearchHistoryFromFirebase(userId: String): List<SearchHistory> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SEARCH_HISTORY)
                .document(userId)
                .collection("queries")
                .orderBy("lastSearched", Query.Direction.DESCENDING)
                .limit(MAX_HISTORY_SIZE.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    SearchHistory.fromFirebaseMap(doc.data ?: return@mapNotNull null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing search history from Firebase", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting search history from Firebase", e)
            getSearchHistoryFromLocal()
        }
    }
    
    private suspend fun removeSearchQueryFromFirebase(userId: String, query: String) {
        try {
            firestore.collection(COLLECTION_SEARCH_HISTORY)
                .document(userId)
                .collection("queries")
                .document(query)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing search query from Firebase: $query", e)
        }
    }
    
    private suspend fun clearSearchHistoryFromFirebase(userId: String) {
        try {
            val batch = firestore.batch()
            val snapshot = firestore.collection(COLLECTION_SEARCH_HISTORY)
                .document(userId)
                .collection("queries")
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing search history from Firebase", e)
        }
    }
    
    private suspend fun cleanupOldFirebaseEntries(userId: String) {
        try {
            val snapshot = firestore.collection(COLLECTION_SEARCH_HISTORY)
                .document(userId)
                .collection("queries")
                .orderBy("lastSearched", Query.Direction.DESCENDING)
                .get()
                .await()
            
            if (snapshot.documents.size > MAX_HISTORY_SIZE) {
                val documentsToDelete = snapshot.documents.drop(MAX_HISTORY_SIZE)
                val batch = firestore.batch()
                
                documentsToDelete.forEach { doc ->
                    batch.delete(doc.reference)
                }
                
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old Firebase entries", e)
        }
    }

    
    private fun addSearchQueryToLocal(query: String) {
        try {
            val currentHistory = getSearchHistoryFromLocal().toMutableList()

            val existingIndex = currentHistory.indexOfFirst { it.query.equals(query, ignoreCase = true) }
            
            if (existingIndex != -1) {
                val existing = currentHistory[existingIndex]
                currentHistory[existingIndex] = existing.copy(
                    searchCount = existing.searchCount + 1,
                    lastSearched = System.currentTimeMillis()
                )
            } else {
                val newEntry = SearchHistory(
                    userId = "local",
                    query = query,
                    searchCount = 1,
                    lastSearched = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                currentHistory.add(0, newEntry)
            }

            val limitedHistory = currentHistory
                .sortedByDescending { it.lastSearched }
                .take(MAX_HISTORY_SIZE)
            
            saveSearchHistoryToLocal(limitedHistory)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding search query to local storage: $query", e)
        }
    }
    
    private fun getSearchHistoryFromLocal(): List<SearchHistory> {
        return try {
            val jsonString = sharedPreferences.getString(KEY_SEARCH_HISTORY, null)
            if (jsonString != null) {
                parseSearchHistoryFromJson(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting search history from local storage", e)
            emptyList()
        }
    }
    
    private fun removeSearchQueryFromLocal(query: String) {
        try {
            val currentHistory = getSearchHistoryFromLocal().toMutableList()
            currentHistory.removeAll { it.query.equals(query, ignoreCase = true) }
            saveSearchHistoryToLocal(currentHistory)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing search query from local storage: $query", e)
        }
    }
    
    private fun clearSearchHistoryFromLocal() {
        try {
            sharedPreferences.edit()
                .remove(KEY_SEARCH_HISTORY)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing search history from local storage", e)
        }
    }
    
    private fun saveSearchHistoryToLocal(history: List<SearchHistory>) {
        try {
            val jsonString = convertSearchHistoryToJson(history)
            sharedPreferences.edit()
                .putString(KEY_SEARCH_HISTORY, jsonString)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving search history to local storage", e)
        }
    }
    
    private fun convertSearchHistoryToJson(history: List<SearchHistory>): String {
        val jsonArray = JSONArray()
        
        history.forEach { searchHistory ->
            val jsonObject = JSONObject().apply {
                put("userId", searchHistory.userId)
                put("query", searchHistory.query)
                put("searchCount", searchHistory.searchCount)
                put("lastSearched", searchHistory.lastSearched)
                put("createdAt", searchHistory.createdAt)
            }
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
    
    private fun parseSearchHistoryFromJson(jsonString: String): List<SearchHistory> {
        val searchHistoryList = mutableListOf<SearchHistory>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val searchHistory = SearchHistory(
                    userId = jsonObject.optString("userId", "local"),
                    query = jsonObject.optString("query", ""),
                    searchCount = jsonObject.optInt("searchCount", 1),
                    lastSearched = jsonObject.optLong("lastSearched", System.currentTimeMillis()),
                    createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
                )
                
                if (searchHistory.query.isNotEmpty()) {
                    searchHistoryList.add(searchHistory)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing search history from JSON", e)
        }
        
        return searchHistoryList
    }
}
