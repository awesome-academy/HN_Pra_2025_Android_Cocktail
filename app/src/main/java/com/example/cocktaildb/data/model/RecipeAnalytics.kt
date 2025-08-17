package com.example.cocktaildb.data.model

data class RecipeView(
    val id: String = "",
    val recipeId: String = "",
    val uid: String? = null,
    val viewedAt: Long = System.currentTimeMillis(),
    val viewDurationSeconds: Int = 0,
    val source: String = ""
)

data class RecipeShare(
    val id: String = "",
    val recipeId: String = "",
    val uid: String = "",
    val platform: String = "",
    val sharedAt: Long = System.currentTimeMillis()
)

data class RecipeComment(
    val id: String = "",
    val recipeId: String = "",
    val uid: String = "",
    val commentText: String = "",
    val rating: Float = 0.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false
)

data class RecipeCollection(
    val id: String = "",
    val uid: String = "",
    val name: String = "",
    val description: String = "",
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class RecipeCollectionItem(
    val id: String = "",
    val collectionId: String = "",
    val recipeId: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
