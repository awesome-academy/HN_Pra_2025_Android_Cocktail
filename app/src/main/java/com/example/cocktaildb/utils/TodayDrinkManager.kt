package com.example.cocktaildb.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.service.CocktailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TodayDrinkManager(
    private val prefs: SharedPreferences
) {
    
    companion object {
        private const val KEY_TODAY_DRINK = "today_drink"
        private const val KEY_LAST_UPDATE_DATE = "last_update_date"
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val JSON_ID_DRINK = "idDrink"
        private const val JSON_STR_DRINK = "strDrink"
        private const val JSON_STR_CATEGORY = "strCategory"
        private const val JSON_STR_ALCOHOLIC = "strAlcoholic"
        private const val JSON_STR_GLASS = "strGlass"
        private const val JSON_STR_INSTRUCTIONS = "strInstructions"
        private const val JSON_STR_DRINK_THUMB = "strDrinkThumb"
        private const val JSON_INGREDIENTS = "ingredients"
        private const val JSON_MEASURES = "measures"
        
        // Factory method for production use
        fun create(context: Context): TodayDrinkManager {
            val prefs = context.getSharedPreferences("today_drink_prefs", Context.MODE_PRIVATE)
            return TodayDrinkManager(prefs)
        }
    }
    
    suspend fun getTodayDrink(): Cocktail? = withContext(Dispatchers.IO) {
        val currentDate = getCurrentDateUTC()
        val lastUpdateDate = prefs.getString(KEY_LAST_UPDATE_DATE, "")

        if (currentDate != lastUpdateDate || !hasSavedDrink()) {
            try {
                val newDrink = CocktailService.getRandomCocktail()
                newDrink?.let { 
                    saveTodayDrink(it, currentDate)
                    return@withContext it
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext getSavedDrink()
            }
        }

        return@withContext getSavedDrink()
    }
    
    private fun getCurrentDateUTC(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(calendar.time)
    }
    
    private fun hasSavedDrink(): Boolean {
        return prefs.contains(KEY_TODAY_DRINK)
    }
    
    private fun getSavedDrink(): Cocktail? {
        val drinkJson = prefs.getString(KEY_TODAY_DRINK, null)
        return if (drinkJson != null) {
            try {
                cocktailFromJson(drinkJson)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    private fun saveTodayDrink(drink: Cocktail, date: String) {
        val drinkJson = cocktailToJson(drink)
        prefs.edit()
            .putString(KEY_TODAY_DRINK, drinkJson)
            .putString(KEY_LAST_UPDATE_DATE, date)
            .apply()
    }

    private fun cocktailToJson(cocktail: Cocktail): String {
        val json = JSONObject().apply {
            put(JSON_ID_DRINK, cocktail.idDrink)
            put(JSON_STR_DRINK, cocktail.strDrink)
            put(JSON_STR_CATEGORY, cocktail.strCategory)
            put(JSON_STR_ALCOHOLIC, cocktail.strAlcoholic)
            put(JSON_STR_GLASS, cocktail.strGlass)
            put(JSON_STR_INSTRUCTIONS, cocktail.strInstructions)
            put(JSON_STR_DRINK_THUMB, cocktail.strDrinkThumb ?: "")
            
            // Convert ingredients list to JSONArray
            val ingredientsArray = JSONArray()
            cocktail.ingredients.forEach { ingredient ->
                ingredientsArray.put(ingredient)
            }
            put(JSON_INGREDIENTS, ingredientsArray)
            
            // Convert measures list to JSONArray
            val measuresArray = JSONArray()
            cocktail.measures.forEach { measure ->
                measuresArray.put(measure)
            }
            put(JSON_MEASURES, measuresArray)
        }
        return json.toString()
    }

    private fun cocktailFromJson(jsonString: String): Cocktail {
        val json = JSONObject(jsonString)
        
        // Parse ingredients array
        val ingredients = mutableListOf<String>()
        val ingredientsArray = json.getJSONArray(JSON_INGREDIENTS)
        for (i in 0 until ingredientsArray.length()) {
            ingredients.add(ingredientsArray.getString(i))
        }
        
        // Parse measures array
        val measures = mutableListOf<String>()
        val measuresArray = json.getJSONArray(JSON_MEASURES)
        for (i in 0 until measuresArray.length()) {
            measures.add(measuresArray.getString(i))
        }
        
        return Cocktail(
            idDrink = json.getString(JSON_ID_DRINK),
            strDrink = json.getString(JSON_STR_DRINK),
            strCategory = json.getString(JSON_STR_CATEGORY),
            strAlcoholic = json.getString(JSON_STR_ALCOHOLIC),
            strGlass = json.getString(JSON_STR_GLASS),
            strInstructions = json.getString(JSON_STR_INSTRUCTIONS),
            strDrinkThumb = json.getString(JSON_STR_DRINK_THUMB).takeIf { it.isNotEmpty() },
            ingredients = ingredients,
            measures = measures
        )
    }

    fun forceRefresh() {
        prefs.edit()
            .remove(KEY_TODAY_DRINK)
            .remove(KEY_LAST_UPDATE_DATE)
            .apply()
    }
}
