package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseRepository {
    private val userService = UserFirebaseService()
    private val favoriteService = FavoriteFirebaseService()
    private val checkmarkService = CheckmarkFirebaseService()
    private val cocktailService = CocktailFirebaseService()
    private val recipeService = RecipeFirebaseService()
    private val historyService = HistoryFirebaseService()

    suspend fun createUser(user: User): Result<String> = withContext(Dispatchers.IO) {
        userService.createUser(user)
    }

    suspend fun getUser(userId: String): Result<User?> = withContext(Dispatchers.IO) {
        userService.getUser(userId)
    }

    suspend fun updateUser(user: User): Result<Boolean> = withContext(Dispatchers.IO) {
        userService.updateUser(user)
    }

    suspend fun deleteUser(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        userService.deleteUser(userId)
    }

    suspend fun addFavorite(userId: String, cocktailId: String): Result<String> = withContext(Dispatchers.IO) {
        favoriteService.addFavorite(userId, cocktailId)
    }

    suspend fun removeFavorite(userId: String, cocktailId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        favoriteService.removeFavorite(userId, cocktailId)
    }

    suspend fun getUserFavorites(userId: String): Result<List<Favorite>> = withContext(Dispatchers.IO) {
        favoriteService.getUserFavorites(userId)
    }

    suspend fun isFavorite(userId: String, cocktailId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        favoriteService.isFavorite(userId, cocktailId)
    }

    suspend fun toggleCheckmark(userId: String, cocktailId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        checkmarkService.toggleCheckmark(userId, cocktailId)
    }

    suspend fun isCheckmarked(userId: String, cocktailId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        checkmarkService.isCheckmarked(userId, cocktailId)
    }

    suspend fun getUserCheckmarks(userId: String): Result<List<Checkmark>> = withContext(Dispatchers.IO) {
        checkmarkService.getUserCheckmarks(userId)
    }

    suspend fun createCocktail(cocktail: CocktailTable): Result<String> = withContext(Dispatchers.IO) {
        cocktailService.createCocktail(cocktail)
    }

    suspend fun getCocktail(cocktailId: String): Result<CocktailTable?> = withContext(Dispatchers.IO) {
        cocktailService.getCocktail(cocktailId)
    }

    suspend fun getAllCocktails(): Result<List<CocktailTable>> = withContext(Dispatchers.IO) {
        cocktailService.getAllCocktails()
    }

    suspend fun searchCocktails(query: String): Result<List<CocktailTable>> = withContext(Dispatchers.IO) {
        cocktailService.searchCocktails(query)
    }

    suspend fun getPopularCocktails(limit: Int = 20): Result<List<CocktailTable>> = withContext(Dispatchers.IO) {
        cocktailService.getPopularCocktails(limit)
    }

    suspend fun updateCocktail(cocktail: CocktailTable): Result<Boolean> = withContext(Dispatchers.IO) {
        cocktailService.updateCocktail(cocktail)
    }

    suspend fun deleteCocktail(cocktailId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        cocktailService.deleteCocktail(cocktailId)
    }

    suspend fun createRecipe(recipe: Recipe): Result<String> = withContext(Dispatchers.IO) {
        recipeService.createRecipe(recipe)
    }

    suspend fun getRecipe(recipeId: String): Result<Recipe?> = withContext(Dispatchers.IO) {
        recipeService.getRecipe(recipeId)
    }

    suspend fun getUserRecipes(userId: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        recipeService.getUserRecipes(userId)
    }

    suspend fun getPublicRecipes(): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        recipeService.getPublicRecipes()
    }

    suspend fun getPopularRecipes(limit: Int = 20): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        recipeService.getPopularRecipes(limit)
    }

    suspend fun searchRecipes(query: String): Result<List<Recipe>> = withContext(Dispatchers.IO) {
        recipeService.searchRecipes(query)
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Boolean> = withContext(Dispatchers.IO) {
        recipeService.updateRecipe(recipe)
    }

    suspend fun deleteRecipe(recipeId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        recipeService.deleteRecipe(recipeId)
    }

    suspend fun incrementRecipeViewCount(recipeId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        recipeService.incrementViewCount(recipeId)
    }

    suspend fun addRecipeImage(recipeImage: RecipeImage): Result<String> = withContext(Dispatchers.IO) {
        recipeService.addRecipeImage(recipeImage)
    }

    suspend fun getRecipeImages(recipeId: String): Result<List<RecipeImage>> = withContext(Dispatchers.IO) {
        recipeService.getRecipeImages(recipeId)
    }

    suspend fun addRecipeIngredient(ingredient: RecipeIngredient): Result<String> = withContext(Dispatchers.IO) {
        recipeService.addRecipeIngredient(ingredient)
    }

    suspend fun getRecipeIngredients(recipeId: String): Result<List<RecipeIngredient>> = withContext(Dispatchers.IO) {
        recipeService.getRecipeIngredients(recipeId)
    }

    suspend fun getSimilarRecipes(recipeId: String): Result<List<SimilarRecipe>> = withContext(Dispatchers.IO) {
        recipeService.getSimilarRecipes(recipeId)
    }

    suspend fun addHistory(userId: String, cocktailId: String): Result<String> = withContext(Dispatchers.IO) {
        historyService.addHistory(userId, cocktailId)
    }

    suspend fun getUserHistory(userId: String): Result<List<History>> = withContext(Dispatchers.IO) {
        historyService.getUserHistory(userId)
    }

    suspend fun getRecentHistory(userId: String, limit: Int = 10): Result<List<History>> = withContext(Dispatchers.IO) {
        historyService.getRecentHistory(userId, limit)
    }

    suspend fun clearUserHistory(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        historyService.clearUserHistory(userId)
    }

    suspend fun getUserStats(userId: String): Result<UserStats> = withContext(Dispatchers.IO) {
        try {
            val favoritesResult = favoriteService.getUserFavorites(userId)
            val checkmarksResult = checkmarkService.getUserCheckmarks(userId)
            val recipesResult = recipeService.getUserRecipes(userId)
            val historyResult = historyService.getUserHistory(userId)

            if (favoritesResult.isSuccess && checkmarksResult.isSuccess && 
                recipesResult.isSuccess && historyResult.isSuccess) {
                
                val stats = UserStats(
                    userId = userId,
                    favoriteCount = favoritesResult.getOrNull()?.size ?: 0,
                    checkmarkCount = checkmarksResult.getOrNull()?.size ?: 0,
                    recipeCount = recipesResult.getOrNull()?.size ?: 0,
                    historyCount = historyResult.getOrNull()?.size ?: 0
                )
                Result.success(stats)
            } else {
                Result.failure(Exception("Failed to get user stats"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class UserStats(
    val userId: String,
    val favoriteCount: Int,
    val checkmarkCount: Int,
    val recipeCount: Int,
    val historyCount: Int
)
