package com.example.cocktaildb.data.model

data class Cocktail(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null
)
