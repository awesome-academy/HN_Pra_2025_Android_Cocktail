package com.example.cocktaildb.data.model

import com.google.firebase.firestore.PropertyName

data class Recipe(
    val id: String = "",
    val uid: String = "",
    val name: String = "",
    val description: String = "",
    val instructions: String = "",
    val prepTimeMinutes: Int = 0,
    val difficultyLevel: String = "",
    val category: String = "",
    val alcoholic: String = "",
    val servings: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("public") @set:PropertyName("public")
    var isPublic: Boolean = true,
    val likeCount: Int = 0,
    val viewCount: Int = 0
)

data class RecipeImage(
    val id: String = "",
    val recipeId: String = "",
    val imageUrl: String = "",
    val isPrimary: Boolean = false,
    val caption: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)

data class RecipeIngredient(
    val id: String = "",
    val recipeId: String = "",
    val ingredientName: String = "",
    val quantity: String = "",
    val unit: String = "",
    val isOptional: Boolean = false,
    val notes: String = ""
)


data class SimilarRecipe(
    val id: String = "",
    val recipeId: String = "",
    val similarRecipeId: String = "",
    val similarityScore: Float = 0.0f,
    val similarityReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class RecipeTag(
    val id: String = "",
    val recipeId: String = "",
    val tagName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
