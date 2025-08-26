package com.example.cocktaildb.data.model

data class SearchHistory(
    val id: String = "",
    val userId: String = "",
    val query: String = "",
    val searchCount: Int = 1,
    val lastSearched: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", 1, System.currentTimeMillis(), System.currentTimeMillis())

    fun toFirebaseMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "query" to query,
            "searchCount" to searchCount,
            "lastSearched" to lastSearched,
            "createdAt" to createdAt
        )
    }
    
    companion object {
        fun fromFirebaseMap(data: Map<String, Any>): SearchHistory {
            return SearchHistory(
                userId = data["userId"] as? String ?: "",
                query = data["query"] as? String ?: "",
                searchCount = (data["searchCount"] as? Number)?.toInt() ?: 1,
                lastSearched = (data["lastSearched"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
