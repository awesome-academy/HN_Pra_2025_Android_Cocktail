package com.example.cocktaildb.data.model

data class History(
    val id: String = "",
    val uid: String = "",
    val cocktailId: String = "",
    val viewedAt: Long = System.currentTimeMillis()
)
