package com.example.cocktaildb.data.repository.source.remote

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.CocktailResponse
import com.example.cocktaildb.data.service.CocktailService
import com.example.cocktaildb.data.repository.source.CocktailDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class CocktailRemoteDataSource : CocktailDataSource {

    companion object {
        private const val TAG = "CocktailRemoteDataSource"
        private const val API_URL = "https://www.thecocktaildb.com/api/json/v1/1/search.php?f=a"
    }

    override fun getCocktails(): List<Cocktail> {
        return CocktailService.searchByName("")
    }

    override fun getCocktailById(id: String): Cocktail? {
        val cocktails = CocktailService.searchByName("")
        return cocktails.find { it.idDrink == id }
    }

    override fun searchCocktails(query: String): List<Cocktail> {
        return CocktailService.searchCombined(query)
    }

    override fun filterByCategory(category: String): List<Cocktail> {
        return CocktailService.filterByCategory(category)
    }

    override fun filterByAlcoholic(alcoholic: String): List<Cocktail> {
        return CocktailService.filterByAlcoholic(alcoholic)
    }

    override fun getCategories(): List<String> {
        return CocktailService.getCategories()
    }

    override suspend fun fetchCocktailsFromApi(): List<Cocktail> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching cocktails from API: $API_URL")

            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d(TAG, "API Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                Log.d(TAG, "API Response received, length: ${response.length}")

                // Parse JSON response
                val jsonObject = JSONObject(response.toString())
                val drinksArray = jsonObject.optJSONArray("drinks")

                if (drinksArray != null) {
                    val cocktails = mutableListOf<Cocktail>()
                    for (i in 0 until drinksArray.length()) {
                        val drinkObject = drinksArray.getJSONObject(i)
                        val cocktail = parseCocktailFromJson(drinkObject)
                        cocktails.add(cocktail)
                    }

                    Log.d(TAG, "Successfully parsed ${cocktails.size} cocktails")
                    return@withContext cocktails
                } else {
                    Log.w(TAG, "No drinks array found in API response")
                    return@withContext emptyList()
                }
            } else {
                Log.e(TAG, "API request failed with response code: $responseCode")
                return@withContext emptyList()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cocktails from API", e)
            return@withContext emptyList()
        } finally {
            Log.d(TAG, "API call completed")
        }
    }

    private fun parseCocktailFromJson(jsonObject: JSONObject): Cocktail {
        // Parse ingredients
        val ingredients = mutableListOf<String>()
        for (i in 1..15) {
            val ingredient = jsonObject.optString("strIngredient$i")
            if (ingredient.isNotEmpty()) {
                ingredients.add(ingredient)
            }
        }

        // Parse measures
        val measures = mutableListOf<String>()
        for (i in 1..15) {
            val measure = jsonObject.optString("strMeasure$i")
            if (measure.isNotEmpty()) {
                measures.add(measure)
            }
        }

        return Cocktail(
            idDrink = jsonObject.optString("idDrink", ""),
            strDrink = jsonObject.optString("strDrink", ""),
            strDrinkAlternate = jsonObject.optString("strDrinkAlternate"),
            strTags = jsonObject.optString("strTags"),
            strVideo = jsonObject.optString("strVideo"),
            strCategory = jsonObject.optString("strCategory"),
            strIBA = jsonObject.optString("strIBA"),
            strAlcoholic = jsonObject.optString("strAlcoholic"),
            strGlass = jsonObject.optString("strGlass"),
            strInstructions = jsonObject.optString("strInstructions"),
            strInstructionsES = jsonObject.optString("strInstructionsES"),
            strInstructionsDE = jsonObject.optString("strInstructionsDE"),
            strInstructionsFR = jsonObject.optString("strInstructionsFR"),
            strInstructionsIT = jsonObject.optString("strInstructionsIT"),
            strInstructionsZH_HANS = jsonObject.optString("strInstructionsZH-HANS"),
            strInstructionsZH_HANT = jsonObject.optString("strInstructionsZH-HANT"),
            strDrinkThumb = jsonObject.optString("strDrinkThumb"),
            ingredients = ingredients,
            measures = measures,
            strImageSource = jsonObject.optString("strImageSource"),
            strImageAttribution = jsonObject.optString("strImageAttribution"),
            strCreativeCommonsConfirmed = jsonObject.optString("strCreativeCommonsConfirmed"),
            dateModified = jsonObject.optString("dateModified")
        )
    }
}

