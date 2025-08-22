package com.example.cocktaildb.data.repository

import android.content.Context
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.source.CocktailDataSource
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.data.manager.FavoritesManager

class CocktailRepository(private val dataSource: CocktailDataSource = CocktailRemoteDataSource()) {

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

    fun preloadOfflineFavorites(context: Context) {
        FavoritesManager.preloadOfflineFavorites(context)
    }

    fun getOfflineFavorites(context: Context): List<Cocktail> {
        return FavoritesManager.getOfflineFavorites(context)
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
    

}
