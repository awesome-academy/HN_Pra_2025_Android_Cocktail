package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.data.model.RecipeImage
import com.example.cocktaildb.data.model.RecipeIngredient
import com.example.cocktaildb.data.model.SimilarRecipe
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RecipeFirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val recipesCollection = firestore.collection("recipes")
    private val recipeImagesCollection = firestore.collection("recipe_images")
    private val recipeIngredientsCollection = firestore.collection("recipe_ingredients")
    private val similarRecipesCollection = firestore.collection("similar_recipes")
    suspend fun createRecipe(recipe: Recipe): Result<String> {
        return try {
            val recipeId = if (recipe.id.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                recipe.id
            }
            val newRecipe = recipe.copy(
                id = recipeId,
                updatedAt = System.currentTimeMillis()
            )
            recipesCollection.document(recipeId).set(newRecipe).await()
            Result.success(recipeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipe(recipeId: String): Result<Recipe?> {
        return try {
            val document = recipesCollection.document(recipeId).get().await()
            if (document.exists()) {
                val recipe = document.toObject(Recipe::class.java)
                Result.success(recipe)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllRecipes(): Result<List<Recipe>> {
        return try {
            val querySnapshot = recipesCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val recipes = querySnapshot.toObjects(Recipe::class.java)
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRecipes(uid: String): Result<List<Recipe>> {
        return try {
            val querySnapshot = recipesCollection
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val recipes = querySnapshot.toObjects(Recipe::class.java)
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicRecipes(): Result<List<Recipe>> {
        return try {
            val querySnapshot = recipesCollection
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val recipes = querySnapshot.toObjects(Recipe::class.java)
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularRecipes(limit: Int = 20): Result<List<Recipe>> {
        return try {
            val querySnapshot = recipesCollection
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()
            val recipes = querySnapshot.toObjects(Recipe::class.java)
            Result.success(recipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchRecipes(searchQuery: String): Result<List<Recipe>> {
        return try {
            val nameResults = recipesCollection
                .whereEqualTo("isPublic", true)
                .whereGreaterThanOrEqualTo("name", searchQuery)
                .whereLessThanOrEqualTo("name", searchQuery + "\uf8ff")
                .get().await()

            val categoryResults = recipesCollection
                .whereEqualTo("isPublic", true)
                .whereEqualTo("category", searchQuery)
                .get().await()

            val recipes = mutableSetOf<Recipe>()
            recipes.addAll(nameResults.toObjects(Recipe::class.java))
            recipes.addAll(categoryResults.toObjects(Recipe::class.java))

            Result.success(recipes.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Boolean> {
        return try {
            val updatedRecipe = recipe.copy(updatedAt = System.currentTimeMillis())
            recipesCollection.document(recipe.id).set(updatedRecipe).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementViewCount(recipeId: String): Result<Boolean> {
        return try {
            firestore.runTransaction { transaction ->
                val docRef = recipesCollection.document(recipeId)
                val snapshot = transaction.get(docRef)
                val currentCount = snapshot.getLong("viewCount") ?: 0
                transaction.update(docRef, "viewCount", currentCount + 1)
            }.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Boolean> {
        return try {
            recipesCollection.document(recipeId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRecipeImage(recipeImage: RecipeImage): Result<String> {
        return try {
            val imageId = if (recipeImage.id.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                recipeImage.id
            }

            val newImage = recipeImage.copy(id = imageId)
            recipeImagesCollection.document(imageId).set(newImage).await()
            Result.success(imageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipeImages(recipeId: String): Result<List<RecipeImage>> {
        return try {
            val querySnapshot = recipeImagesCollection
                .whereEqualTo("recipeId", recipeId)
                .orderBy("uploadedAt", Query.Direction.ASCENDING)
                .get().await()
            val images = querySnapshot.toObjects(RecipeImage::class.java)
            Result.success(images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecipeImage(imageId: String): Result<Boolean> {
        return try {
            recipeImagesCollection.document(imageId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun addRecipeIngredient(ingredient: RecipeIngredient): Result<String> {
        return try {
            val ingredientId = if (ingredient.id.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                ingredient.id
            }

            val newIngredient = ingredient.copy(id = ingredientId)
            recipeIngredientsCollection.document(ingredientId).set(newIngredient).await()
            Result.success(ingredientId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipeIngredients(recipeId: String): Result<List<RecipeIngredient>> {
        return try {
            val querySnapshot = recipeIngredientsCollection
                .whereEqualTo("recipeId", recipeId)
                .get().await()
            val ingredients = querySnapshot.toObjects(RecipeIngredient::class.java)
            Result.success(ingredients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecipeIngredient(ingredientId: String): Result<Boolean> {
        return try {
            recipeIngredientsCollection.document(ingredientId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSimilarRecipe(similarRecipe: SimilarRecipe): Result<String> {
        return try {
            val similarId = if (similarRecipe.id.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                similarRecipe.id
            }

            val newSimilar = similarRecipe.copy(id = similarId)
            similarRecipesCollection.document(similarId).set(newSimilar).await()
            Result.success(similarId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSimilarRecipes(recipeId: String): Result<List<SimilarRecipe>> {
        return try {
            val querySnapshot = similarRecipesCollection
                .whereEqualTo("recipeId", recipeId)
                .orderBy("similarityScore", Query.Direction.DESCENDING)
                .get().await()
            val similarRecipes = querySnapshot.toObjects(SimilarRecipe::class.java)
            Result.success(similarRecipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSimilarRecipe(similarId: String): Result<Boolean> {
        return try {
            similarRecipesCollection.document(similarId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
