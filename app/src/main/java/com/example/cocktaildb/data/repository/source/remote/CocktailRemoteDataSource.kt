package com.example.cocktaildb.data.repository.source.remote

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.CocktailResponse
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
        // Return empty list for local method
        return emptyList()
    }

    override fun getCocktailById(id: String): Cocktail? {
        // Return null for local method
        return null
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
            strIngredient1 = jsonObject.optString("strIngredient1"),
            strIngredient2 = jsonObject.optString("strIngredient2"),
            strIngredient3 = jsonObject.optString("strIngredient3"),
            strIngredient4 = jsonObject.optString("strIngredient4"),
            strIngredient5 = jsonObject.optString("strIngredient5"),
            strIngredient6 = jsonObject.optString("strIngredient6"),
            strIngredient7 = jsonObject.optString("strIngredient7"),
            strIngredient8 = jsonObject.optString("strIngredient8"),
            strIngredient9 = jsonObject.optString("strIngredient9"),
            strIngredient10 = jsonObject.optString("strIngredient10"),
            strIngredient11 = jsonObject.optString("strIngredient11"),
            strIngredient12 = jsonObject.optString("strIngredient12"),
            strIngredient13 = jsonObject.optString("strIngredient13"),
            strIngredient14 = jsonObject.optString("strIngredient14"),
            strIngredient15 = jsonObject.optString("strIngredient15"),
            strMeasure1 = jsonObject.optString("strMeasure1"),
            strMeasure2 = jsonObject.optString("strMeasure2"),
            strMeasure3 = jsonObject.optString("strMeasure3"),
            strMeasure4 = jsonObject.optString("strMeasure4"),
            strMeasure5 = jsonObject.optString("strMeasure5"),
            strMeasure6 = jsonObject.optString("strMeasure6"),
            strMeasure7 = jsonObject.optString("strMeasure7"),
            strMeasure8 = jsonObject.optString("strMeasure8"),
            strMeasure9 = jsonObject.optString("strMeasure9"),
            strMeasure10 = jsonObject.optString("strMeasure10"),
            strMeasure11 = jsonObject.optString("strMeasure11"),
            strMeasure12 = jsonObject.optString("strMeasure12"),
            strMeasure13 = jsonObject.optString("strMeasure13"),
            strMeasure14 = jsonObject.optString("strMeasure14"),
            strMeasure15 = jsonObject.optString("strMeasure15"),
            strImageSource = jsonObject.optString("strImageSource"),
            strImageAttribution = jsonObject.optString("strImageAttribution"),
            strCreativeCommonsConfirmed = jsonObject.optString("strCreativeCommonsConfirmed"),
            dateModified = jsonObject.optString("dateModified")
        )
    }
}

