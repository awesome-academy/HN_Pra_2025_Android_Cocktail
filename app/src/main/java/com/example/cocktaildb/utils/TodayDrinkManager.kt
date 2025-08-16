package com.example.cocktaildb.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.service.CocktailService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TodayDrinkManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("today_drink_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_TODAY_DRINK = "today_drink"
        private const val KEY_LAST_UPDATE_DATE = "last_update_date"
        private const val DATE_FORMAT = "yyyy-MM-dd"
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
                gson.fromJson(drinkJson, Cocktail::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    private fun saveTodayDrink(drink: Cocktail, date: String) {
        val drinkJson = gson.toJson(drink)
        prefs.edit()
            .putString(KEY_TODAY_DRINK, drinkJson)
            .putString(KEY_LAST_UPDATE_DATE, date)
            .apply()
    }

    fun forceRefresh() {
        prefs.edit()
            .remove(KEY_TODAY_DRINK)
            .remove(KEY_LAST_UPDATE_DATE)
            .apply()
    }
}
