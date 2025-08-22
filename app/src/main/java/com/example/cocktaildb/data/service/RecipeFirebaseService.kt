package com.example.cocktaildb.data.service

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
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
            val querySnapshot = recipesCollection.get().await()
            val recipes = querySnapshot.toObjects(Recipe::class.java)
            val sortedRecipes = recipes.sortedByDescending { it.createdAt }
            Result.success(sortedRecipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRecipes(uid: String): Result<List<Recipe>> {
        return try {
            val querySnapshot = recipesCollection
                .whereEqualTo("uid", uid)
                .get().await()
            val recipes = querySnapshot.toObjects(Recipe::class.java)
            val sortedRecipes = recipes.sortedByDescending { it.createdAt }
            Result.success(sortedRecipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicRecipes(): Result<List<Recipe>> {
        return try {
            val querySnapshot = recipesCollection
                .whereEqualTo("public", true)
                .get().await()
            val recipes = querySnapshot.toObjects(Recipe::class.java)
            val sortedRecipes = recipes.sortedByDescending { it.createdAt }
            Result.success(sortedRecipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularRecipes(limit: Int = 20): Result<List<Recipe>> {
        return try {
            val querySnapshot = recipesCollection
                .whereEqualTo("public", true)
                .get().await()
            val recipes = querySnapshot.toObjects(Recipe::class.java)
            val sortedRecipes = recipes.sortedByDescending { it.createdAt }.take(limit)
            Result.success(sortedRecipes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchRecipes(searchQuery: String): Result<List<Recipe>> {
        return try {
            val nameResults = recipesCollection
                .whereEqualTo("public", true)
                .whereGreaterThanOrEqualTo("name", searchQuery)
                .whereLessThanOrEqualTo("name", searchQuery + "\uf8ff")
                .get().await()

            val categoryResults = recipesCollection
                .whereEqualTo("public", true)
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
                .get().await()
            val images = querySnapshot.toObjects(RecipeImage::class.java)
            val sortedImages = images.sortedBy { it.uploadedAt }
            Result.success(sortedImages)
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
                .get().await()
            val similar = querySnapshot.toObjects(SimilarRecipe::class.java)
            val sortedSimilar = similar.sortedByDescending { it.similarityScore }
            Result.success(sortedSimilar)
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

    suspend fun cleanupInvalidImageUrls(): Result<Int> {
        return try {
            val querySnapshot = recipeImagesCollection.get().await()
            var cleanedCount = 0
            
            querySnapshot.documents.forEach { document ->
                val imageUrl = document.getString("imageUrl") ?: ""
                
                if (imageUrl.contains("com.miui.gallery.open") || 
                    imageUrl.contains("gallery.open") ||
                    (imageUrl.startsWith("content://") && !imageUrl.startsWith("file://"))) {
                    document.reference.delete()
                    cleanedCount++
                }
            }
            
            Result.success(cleanedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW METHODS FOR SHARED RECIPES (using Firestore collections: recipes, recipe_ingredients, recipe_images)
    suspend fun getSharedRecipes(limit: Int = 20): Result<List<Cocktail>> {
        return try {
            Log.d("RecipeFirebaseService", "getSharedRecipes: fetching $limit recipes from Firestore")
            
            // Avoid composite index by removing orderBy; sort in memory
            val recipesSnapshot = recipesCollection
                .whereEqualTo("public", true)
                .get()
                .await()

            val recipeDatas = recipesSnapshot.toObjects(RecipeData::class.java)
                .sortedByDescending { it.createdAt }
                .take(limit)

            val recipes = mutableListOf<Cocktail>()
            
            for (recipe in recipeDatas) {
                try {
                    val ingredients = getRecipeIngredientsFromFirestore(recipe.id)
                    val imageUrl = getRecipePrimaryImageFromFirestore(recipe.id)
                    val cocktail = Cocktail(
                        idDrink = recipe.id,
                        strDrink = recipe.name,
                        strCategory = recipe.category,
                        strAlcoholic = recipe.alcoholic,
                        strInstructions = recipe.instructions,
                        strDrinkThumb = imageUrl,
                        ingredients = ingredients,
                        measures = List(ingredients.size) { "" }
                    )
                    recipes.add(cocktail)
                    Log.d("RecipeFirebaseService", "getSharedRecipes: mapped recipe '${recipe.name}' with ${ingredients.size} ingredients")
                } catch (e: Exception) {
                    Log.e("RecipeFirebaseService", "getSharedRecipes: error mapping recipe ${recipe.id}", e)
                }
            }
            
            Log.d("RecipeFirebaseService", "getSharedRecipes: successfully fetched ${recipes.size} recipes")
            Result.success(recipes)
            
        } catch (e: Exception) {
            Log.e("RecipeFirebaseService", "getSharedRecipes: error fetching recipes", e)
            Result.failure(e)
        }
    }

    private suspend fun getRecipeIngredientsFromFirestore(recipeId: String): List<String> {
        return try {
            val ingredientsSnapshot = recipeIngredientsCollection
                .whereEqualTo("recipeId", recipeId)
                .get()
                .await()
            
            ingredientsSnapshot.documents.mapNotNull { doc ->
                doc.getString("ingredientName")
            }
        } catch (e: Exception) {
            Log.e("RecipeFirebaseService", "getRecipeIngredientsFromFirestore: error fetching ingredients for recipe $recipeId", e)
            emptyList()
        }
    }

    private suspend fun getRecipePrimaryImageFromFirestore(recipeId: String): String? {
        return try {
            val imageSnapshot = recipeImagesCollection
                .whereEqualTo("recipeId", recipeId)
                .whereEqualTo("primary", true)
                .limit(1)
                .get()
                .await()
            
            imageSnapshot.documents.firstOrNull()?.getString("imageUrl")
        } catch (e: Exception) {
            Log.e("RecipeFirebaseService", "getRecipePrimaryImageFromFirestore: error fetching image for recipe $recipeId", e)
            null
        }
    }

    suspend fun getAllSharedRecipes(): Result<List<Cocktail>> {
        return try {
            Log.d("RecipeFirebaseService", "getAllSharedRecipes: fetching all public recipes")
            
            // Avoid composite index by removing orderBy; sort in memory
            val recipesSnapshot = recipesCollection
                .whereEqualTo("public", true)
                .get()
                .await()

            val recipeDatas = recipesSnapshot.toObjects(RecipeData::class.java)
                .sortedByDescending { it.createdAt }

            val recipes = mutableListOf<Cocktail>()
            
            for (recipe in recipeDatas) {
                try {
                    val ingredients = getRecipeIngredientsFromFirestore(recipe.id)
                    val imageUrl = getRecipePrimaryImageFromFirestore(recipe.id)
                    val cocktail = Cocktail(
                        idDrink = recipe.id,
                        strDrink = recipe.name,
                        strCategory = recipe.category,
                        strAlcoholic = recipe.alcoholic,
                        strInstructions = recipe.instructions,
                        strDrinkThumb = imageUrl,
                        ingredients = ingredients,
                        measures = List(ingredients.size) { "" }
                    )
                    recipes.add(cocktail)
                } catch (e: Exception) {
                    Log.e("RecipeFirebaseService", "getAllSharedRecipes: error mapping recipe ${recipe.id}", e)
                }
            }
            
            Log.d("RecipeFirebaseService", "getAllSharedRecipes: successfully fetched ${recipes.size} recipes")
            Result.success(recipes)
            
        } catch (e: Exception) {
            Log.e("RecipeFirebaseService", "getAllSharedRecipes: error fetching recipes", e)
            Result.failure(e)
        }
    }
}

// Data class for Firestore recipe document
data class RecipeData(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val alcoholic: String = "",
    val instructions: String = "",
    val createdAt: Long = 0,
    val public: Boolean = false,
    val uid: String = ""
)


