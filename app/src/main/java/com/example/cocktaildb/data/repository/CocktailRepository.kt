package com.example.cocktaildb.data.repository

import android.content.Context
import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.source.CocktailDataSource
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.data.manager.FavoritesManager
import com.example.cocktaildb.data.service.FavoriteFirebaseService
import com.example.cocktaildb.data.service.CheckmarkFirebaseService
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

class CocktailRepository(
    private val dataSource: CocktailDataSource = CocktailRemoteDataSource(),
    private val favoriteFirebaseService: FavoriteFirebaseService = FavoriteFirebaseService(),
    private val checkmarkFirebaseService: CheckmarkFirebaseService = CheckmarkFirebaseService()
) {

    fun getCocktails(): List<Cocktail> {
        return dataSource.getAllCocktails()
    }

    fun getAllCocktails(): List<Cocktail> {
        return dataSource.getAllCocktails()
    }

    fun getCocktailById(id: String): Cocktail? {
        return dataSource.getCocktailById(id)
    }

    suspend fun fetchCocktailsFromApi(): List<Cocktail> {
        return dataSource.fetchCocktailsFromApi()
    }

    fun searchCocktails(query: String): List<Cocktail> {
        return dataSource.searchCocktails(query)
    }

    fun filterByCategory(category: String): List<Cocktail> {
        return dataSource.filterByCategory(category)
    }

    fun filterByAlcoholic(alcoholic: String): List<Cocktail> {
        return dataSource.filterByAlcoholic(alcoholic)
    }

    fun getCategories(): List<String> {
        return dataSource.getCategories()
    }

    fun loadMoreCocktails(): List<Cocktail> {
        return dataSource.loadMoreCocktails()
    }
    
    fun getAlcoholicTypes(): List<String> {
        return dataSource.getAlcoholicTypes()
    }

    fun getHistoryCocktails(context: Context): List<Cocktail> {
        val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
        val historyString = sharedPreferences.getString("cocktail_history", "") ?: ""
        return if (historyString.isEmpty()) {
            emptyList()
        } else {
            historyString.split("||").mapNotNull { entry ->
                val fields = entry.split("|:|")
                if (fields.size >= 3) {
                    Cocktail(idDrink = fields[0], strDrink = fields[1], strDrinkThumb = fields[2])
                } else null
            }
        }
    }

    fun getHistoryString(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
        return sharedPreferences.getString("cocktail_history", "") ?: ""
    }

    fun clearHistory(context: Context) {
        val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("cocktail_history").apply()
    }

    suspend fun toggleFavorite(context: Context, cocktail: Cocktail): Boolean {
        return suspendCancellableCoroutine { cont ->
            FavoritesManager.toggleFavoriteOfflineAware(context, cocktail) { newState ->
                if (cont.isActive) cont.resume(newState, onCancellation = null)
            }
        }
    }


    fun clearAllFavorites(context: Context) {
        val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    suspend fun clearAllFavoritesFromFirebase(uid: String): Result<Unit> {
        return try {
            val result = favoriteFirebaseService.clearUserFavorites(uid)
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFavoritesFromFirebase(uid: String): Result<List<com.example.cocktaildb.data.model.Favorite>> {
        return favoriteFirebaseService.getUserFavorites(uid)
    }

    private fun encUrl(s: String?): String =
        try {
            URLEncoder.encode(s ?: "", "UTF-8") } catch (_: Exception) { "" }

    private fun decUrl(s: String?): String =
        try {
            URLDecoder.decode(s ?: "", "UTF-8") } catch (_: Exception) { s ?: "" }

    fun getFavoritesFromLocal(context: Context): List<Cocktail> {
        val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        val favoritesString = prefs.getString("favorites_list", "") ?: ""
        if (favoritesString.isEmpty()) return emptyList()

        val SEPARATOR = "|||"
        val FIELD_SEPARATOR = ":::"
        val FIELD_COUNT = 9

        return favoritesString.split(SEPARATOR).mapNotNull { cocktailString ->
            val fields = cocktailString.split(FIELD_SEPARATOR, limit = FIELD_COUNT)
            if (fields.size >= FIELD_COUNT) {
                Cocktail(
                    idDrink         = fields[0],
                    strDrink        = fields[1],
                    strDrinkThumb   = decUrl(fields[2]).ifBlank { null },
                    strCategory     = fields[3].ifBlank { null },
                    strAlcoholic    = fields[4].ifBlank { null },
                    strGlass        = fields[5].ifBlank { null },
                    strInstructions = fields[6].ifBlank { null },
                    ingredients     = if (fields[7].isNotBlank()) listOf(fields[7]) else emptyList(),
                    dateModified    = fields[8].ifBlank { null }
                )
            } else null
        }
    }


    fun saveFavoritesToLocal(context: Context, favorites: List<Cocktail>) {
        val prefs = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        val SEPARATOR = "|||"
        val FIELD_SEPARATOR = ":::"
        val MAX_FAVORITES_ITEMS = 100

        val favoritesString = favorites.take(MAX_FAVORITES_ITEMS).joinToString(SEPARATOR) { cocktail ->
            listOf(
                cocktail.idDrink,
                cocktail.strDrink,
                encUrl(cocktail.strDrinkThumb),
                cocktail.strCategory ?: "",
                cocktail.strAlcoholic ?: "",
                cocktail.strGlass ?: "",
                cocktail.strInstructions ?: "",
                cocktail.ingredients.firstOrNull() ?: "",
                cocktail.dateModified ?: ""
            ).joinToString(FIELD_SEPARATOR)
        }
        prefs.edit().putString("favorites_list", favoritesString).apply()
    }


    // Checkmark functions (similar to favorites)
    suspend fun getUserCheckmarksFromFirebase(uid: String): Result<List<com.example.cocktaildb.data.model.Checkmark>> {
        return checkmarkFirebaseService.getUserCheckmarks(uid)
    }

    fun getCheckmarksFromLocal(context: Context): List<Cocktail> {
        val prefs = context.getSharedPreferences("checkmarks_prefs", Context.MODE_PRIVATE)
        val checkmarksString = prefs.getString("checkmarks_list", "") ?: ""
        if (checkmarksString.isEmpty()) return emptyList()

        val SEPARATOR = "|||"
        val FIELD_SEPARATOR = ":::"
        val FIELD_COUNT = 9

        return checkmarksString.split(SEPARATOR).mapNotNull { cocktailString ->
            val fields = cocktailString.split(FIELD_SEPARATOR, limit = FIELD_COUNT)
            if (fields.size >= FIELD_COUNT) {
                Cocktail(
                    idDrink         = fields[0],
                    strDrink        = fields[1],
                    strDrinkThumb   = decUrl(fields[2]).ifBlank { null },
                    strCategory     = fields[3].ifBlank { null },
                    strAlcoholic    = fields[4].ifBlank { null },
                    strGlass        = fields[5].ifBlank { null },
                    strInstructions = fields[6].ifBlank { null },
                    ingredients     = if (fields[7].isNotBlank()) listOf(fields[7]) else emptyList(),
                    dateModified    = fields[8].ifBlank { null }
                )
            } else null
        }
    }


    fun saveCheckmarksToLocal(context: Context, checkmarks: List<Cocktail>) {
        val prefs = context.getSharedPreferences("checkmarks_prefs", Context.MODE_PRIVATE)
        val SEPARATOR = "|||"
        val FIELD_SEPARATOR = ":::"
        val MAX_CHECKMARKS_ITEMS = 100

        val checkmarksString = checkmarks.take(MAX_CHECKMARKS_ITEMS).joinToString(SEPARATOR) { cocktail ->
            listOf(
                cocktail.idDrink,
                cocktail.strDrink,
                encUrl(cocktail.strDrinkThumb),
                cocktail.strCategory ?: "",
                cocktail.strAlcoholic ?: "",
                cocktail.strGlass ?: "",
                cocktail.strInstructions ?: "",
                cocktail.ingredients.firstOrNull() ?: "",
                cocktail.dateModified ?: ""
            ).joinToString(FIELD_SEPARATOR)
        }
        prefs.edit().putString("checkmarks_list", checkmarksString).apply()
    }


    suspend fun addCheckmarkToFirebase(uid: String, cocktailId: String): Result<Unit> {
        return try {
            val result = checkmarkFirebaseService.addCheckmark(uid, cocktailId)
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCheckmarkFromFirebase(uid: String, cocktailId: String): Result<Unit> {
        return try {
            val result = checkmarkFirebaseService.removeCheckmark(uid, cocktailId)
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllCheckmarksFromFirebase(uid: String): Result<Unit> {
        return try {
            val result = checkmarkFirebaseService.clearUserCheckmarks(uid)
            if (result.isSuccess) Result.success(Unit) else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearAllCheckmarksFromLocal(context: Context) {
        val prefs = context.getSharedPreferences("checkmarks_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun addToFavorites(context: Context, cocktail: Cocktail, callback: (Boolean) -> Unit) {
        // Always add to local first
        FavoritesManager.addFavoriteOffline(context, cocktail)
        // Then try to add to Firestore if possible
        FavoritesManager.addFavoriteToFirestore(cocktail) { success ->
            // Optionally, you can handle sync status here
            callback(true)
        }
    }

    fun removeFromFavorites(context: Context, cocktail: Cocktail, callback: (Boolean) -> Unit) {
        val removedLocal = FavoritesManager.removeFavoriteOffline(context, cocktail)
        Log.d("FavoritesManager", "removeFromFavorites -> Local removed: $removedLocal for ${cocktail.strDrink} (id=${cocktail.idDrink})")

        // Then try to remove from Firestore if possible
        FavoritesManager.removeFavoriteFromFirestore(cocktail) { success ->
            Log.d("FavoritesManager", "Firestore remove result for ${cocktail.strDrink} (id=${cocktail.idDrink}): $success")
            // Optionally, you can handle sync status here
            callback(true)
        }
    }

    fun clearAllLocalData(context: Context) {
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
            sharedPrefsDir.list()?.forEach { prefFile ->
                val prefName = prefFile.removeSuffix(".xml")
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
            }
        }
    }


}
