package com.example.cocktaildb.data.model

data class DataCocktail(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val category: String? = null,
    val alcoholic: String? = null,
    val rating: Float? = null
)

