package com.example.cocktaildb.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.cocktaildb.data.model.*
import com.example.cocktaildb.data.service.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val userFirebaseService = UserFirebaseService()
    private val cocktailFirebaseService = CocktailFirebaseService()
    private val recipeFirebaseService = RecipeFirebaseService()
    private val favoriteFirebaseService = FavoriteFirebaseService()
    private val historyFirebaseService = HistoryFirebaseService()
    private val checkmarkFirebaseService = CheckmarkFirebaseService()
    private val imageUploadService = ImageUploadService()

    // User management
    suspend fun createUser(user: User): Result<String> {
        return userFirebaseService.createUser(user)
    }

    suspend fun getUser(uid: String): Result<User?> {
        return userFirebaseService.getUser(uid)
    }

    suspend fun updateUser(user: User): Result<Boolean> {
        return userFirebaseService.updateUser(user)
    }

    suspend fun deleteUser(uid: String): Result<Boolean> {
        return userFirebaseService.deleteUser(uid)
    }

    // Cocktail management
    suspend fun createCocktail(cocktail: CocktailTable): Result<String> {
        return cocktailFirebaseService.createCocktail(cocktail)
    }

    suspend fun getCocktail(cocktailId: String): Result<CocktailTable?> {
        return cocktailFirebaseService.getCocktail(cocktailId)
    }

    suspend fun getAllCocktails(): Result<List<CocktailTable>> {
        return cocktailFirebaseService.getAllCocktails()
    }

    suspend fun searchCocktails(query: String): Result<List<CocktailTable>> {
        return cocktailFirebaseService.searchCocktails(query)
    }

    suspend fun getPopularCocktails(limit: Int = 20): Result<List<CocktailTable>> {
        return cocktailFirebaseService.getPopularCocktails(limit)
    }

    suspend fun getUserCreatedCocktails(uid: String): Result<List<CocktailTable>> {
        return cocktailFirebaseService.getUserCreatedCocktails(uid)
    }

    suspend fun updateCocktail(cocktail: CocktailTable): Result<Boolean> {
        return cocktailFirebaseService.updateCocktail(cocktail)
    }

    suspend fun deleteCocktail(cocktailId: String): Result<Boolean> {
        return cocktailFirebaseService.deleteCocktail(cocktailId)
    }

    // Recipe management
    suspend fun createRecipe(recipe: Recipe): Result<String> {
        return recipeFirebaseService.createRecipe(recipe)
    }

    suspend fun getRecipe(recipeId: String): Result<Recipe?> {
        return recipeFirebaseService.getRecipe(recipeId)
    }

    suspend fun getAllRecipes(): Result<List<Recipe>> {
        return recipeFirebaseService.getAllRecipes()
    }

    suspend fun getUserRecipes(uid: String): Result<List<Recipe>> {
        return recipeFirebaseService.getUserRecipes(uid)
    }

    suspend fun getPublicRecipes(): Result<List<Recipe>> {
        return recipeFirebaseService.getPublicRecipes()
    }

    suspend fun getPopularRecipes(limit: Int = 20): Result<List<Recipe>> {
        return recipeFirebaseService.getPopularRecipes(limit)
    }

    suspend fun searchRecipes(searchQuery: String): Result<List<Recipe>> {
        return recipeFirebaseService.searchRecipes(searchQuery)
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Boolean> {
        return recipeFirebaseService.updateRecipe(recipe)
    }

    suspend fun incrementViewCount(recipeId: String): Result<Boolean> {
        return recipeFirebaseService.incrementViewCount(recipeId)
    }

    suspend fun deleteRecipe(recipeId: String): Result<Boolean> {
        return recipeFirebaseService.deleteRecipe(recipeId)
    }

    suspend fun addRecipeImage(recipeImage: RecipeImage): Result<String> {
        return recipeFirebaseService.addRecipeImage(recipeImage)
    }

    suspend fun getRecipeImages(recipeId: String): Result<List<RecipeImage>> {
        return recipeFirebaseService.getRecipeImages(recipeId)
    }

    suspend fun addRecipeIngredient(ingredient: RecipeIngredient): Result<String> {
        return recipeFirebaseService.addRecipeIngredient(ingredient)
    }

    suspend fun getRecipeIngredients(recipeId: String): Result<List<RecipeIngredient>> {
        return recipeFirebaseService.getRecipeIngredients(recipeId)
    }

    suspend fun getSimilarRecipes(recipeId: String): Result<List<SimilarRecipe>> {
        return recipeFirebaseService.getSimilarRecipes(recipeId)
    }

    // NEW: Shared recipes from Firestore collections
    suspend fun getSharedRecipes(limit: Int = 20): Result<List<Cocktail>> {
        return recipeFirebaseService.getSharedRecipes(limit)
    }

    suspend fun getAllSharedRecipes(): Result<List<Cocktail>> {
        return recipeFirebaseService.getAllSharedRecipes()
    }

    // Favorite management
    suspend fun addToFavorites(uid: String, cocktailId: String): Result<String> {
        return favoriteFirebaseService.addFavorite(uid, cocktailId)
    }

    suspend fun removeFromFavorites(uid: String, cocktailId: String): Result<Boolean> {
        return favoriteFirebaseService.removeFavorite(uid, cocktailId)
    }

    suspend fun getUserFavorites(uid: String): Result<List<Favorite>> {
        return favoriteFirebaseService.getUserFavorites(uid)
    }

    suspend fun isFavorite(uid: String, cocktailId: String): Result<Boolean> {
        return favoriteFirebaseService.isFavorite(uid, cocktailId)
    }

    // History management
    suspend fun addToHistory(uid: String, cocktailId: String): Result<String> {
        return historyFirebaseService.addHistory(uid, cocktailId)
    }

    suspend fun getUserHistory(uid: String): Result<List<History>> {
        return historyFirebaseService.getUserHistory(uid)
    }

    suspend fun clearUserHistory(uid: String): Result<Boolean> {
        return historyFirebaseService.clearUserHistory(uid)
    }

    // Checkmark management
    suspend fun addCheckmark(uid: String, cocktailId: String): Result<String> {
        return checkmarkFirebaseService.addCheckmark(uid, cocktailId)
    }

    suspend fun removeCheckmark(uid: String, cocktailId: String): Result<Boolean> {
        return checkmarkFirebaseService.removeCheckmark(uid, cocktailId)
    }

    suspend fun getUserCheckmarks(uid: String): Result<List<Checkmark>> {
        return checkmarkFirebaseService.getUserCheckmarks(uid)
    }

    suspend fun isChecked(uid: String, cocktailId: String): Result<Boolean> {
        return checkmarkFirebaseService.isCheckmarked(uid, cocktailId)
    }

    // Image upload
    suspend fun uploadRecipeImage(context: Context, imageUri: Uri, recipeId: String): Result<String> {
        return imageUploadService.uploadRecipeImage(context, imageUri, recipeId)
    }

    suspend fun deleteRecipeImage(imageUrl: String): Result<Boolean> {
        return imageUploadService.deleteRecipeImage(imageUrl)
    }

    fun isImgBBUrl(url: String): Boolean {
        return imageUploadService.isImgBBUrl(url)
    }

    // Analytics
    suspend fun getUserRecipeStats(uid: String): Result<Map<String, Any>> {
        return try {
            val userRecipes = getUserRecipes(uid).getOrNull() ?: emptyList()
            val totalRecipes = userRecipes.size
            val totalViews = userRecipes.sumOf { it.viewCount ?: 0 }
            val totalLikes = userRecipes.sumOf { it.likeCount ?: 0 }
            
            val stats = mapOf(
                "totalRecipes" to totalRecipes,
                "totalViews" to totalViews,
                "totalLikes" to totalLikes
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting user recipe stats", e)
            Result.failure(e)
        }
    }
}
