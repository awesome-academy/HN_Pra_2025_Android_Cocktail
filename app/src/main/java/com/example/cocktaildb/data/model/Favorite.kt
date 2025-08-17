package com.example.cocktaildb.data.model

data class Favorite(
    val id: String = "",
    val uid: String = "",
    val cocktailId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

