package com.example.cocktaildb.data.model

data class CocktailTable(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val alcoholic: String = "",
    val instructions: String = "",
    val thumbnailUrl: String = "",
    val ingredients: List<String> = emptyList(),
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Ingredient(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class CocktailIngredient(
    val id: String = "",
    val cocktailId: String = "",
    val ingredientId: String = "",
    val measure: String = "",
    val isOptional: Boolean = false
)
