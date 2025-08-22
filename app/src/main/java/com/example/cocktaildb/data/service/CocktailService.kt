package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.Cocktail
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.util.Log

object CocktailService {
    private const val BASE = "https://www.thecocktaildb.com/api/json/v1/1/"
    private const val TAG = "CocktailService"

    fun searchByName(query: String): List<Cocktail> {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = BASE + "search.php?s=$q"
        android.util.Log.d(TAG, "searchByName: calling URL: $url")
        val json = get(url)
        android.util.Log.d(TAG, "searchByName: got JSON response length: ${json.length}")
        val cocktails = parseDetailedDrinks(json)
        android.util.Log.d(TAG, "searchByName: parsed ${cocktails.size} cocktails")
        return cocktails
    }

    fun getAllCocktails(): List<Cocktail> {
        // Load all cocktails from all letters for complete collection
        // This will be cached locally for future use
        val letters = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")
        
        android.util.Log.d(TAG, "getAllCocktails: loading all cocktails from all letters for complete collection")
        
        val allCocktails = mutableListOf<Cocktail>()
        
        // Search for cocktails starting with each letter
        for (letter in letters) {
            try {
                val url = BASE + "search.php?f=$letter"
                android.util.Log.d(TAG, "getAllCocktails: calling URL: $url")
                val json = get(url)
                val cocktails = parseDetailedDrinks(json)
                
                if (cocktails.isNotEmpty()) {
                    // Add all cocktails from each letter for complete collection
                    allCocktails.addAll(cocktails)
                    android.util.Log.d(TAG, "getAllCocktails: found ${cocktails.size} cocktails starting with '$letter', total so far: ${allCocktails.size}")
                } else {
                    android.util.Log.w(TAG, "getAllCocktails: no cocktails found starting with '$letter'")
                }
                
                // Small delay to avoid overwhelming the API
                Thread.sleep(30)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "getAllCocktails: error searching for letter '$letter'", e)
            }
        }
        
        android.util.Log.d(TAG, "getAllCocktails: total cocktails collected: ${allCocktails.size}")
        return allCocktails
    }
    
    fun loadMoreCocktails(): List<Cocktail> {
        // Load additional cocktails for pagination
        val letters = listOf("g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")
        
        android.util.Log.d(TAG, "loadMoreCocktails: loading additional cocktails from remaining letters")
        
        val moreCocktails = mutableListOf<Cocktail>()
        
        for (letter in letters) {
            try {
                val url = BASE + "search.php?f=$letter"
                val json = get(url)
                val cocktails = parseDetailedDrinks(json)
                
                if (cocktails.isNotEmpty()) {
                    val cocktailsToAdd = if (cocktails.size > 3) cocktails.take(3) else cocktails
                    moreCocktails.addAll(cocktailsToAdd)
                }
                
                Thread.sleep(20)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "loadMoreCocktails: error searching for letter '$letter'", e)
            }
        }
        
        android.util.Log.d(TAG, "loadMoreCocktails: additional cocktails loaded: ${moreCocktails.size}")
        return moreCocktails
    }

    fun searchByIngredient(ingredient: String): List<Cocktail> {
        val i = URLEncoder.encode(ingredient, "UTF-8")
        val url = BASE + "filter.php?i=$i"
        val json = get(url)
        val filteredDrinks = parseFilteredDrinks(json)
        val detailedDrinks = mutableListOf<Cocktail>()
        for (drink in filteredDrinks) {
            val detailUrl = BASE + "lookup.php?i=${drink.idDrink}"
            val detailJson = get(detailUrl)
            val detailedList = parseDetailedDrinks(detailJson)
            if (detailedList.isNotEmpty()) {
                detailedDrinks.add(detailedList[0])
            }
        }
        
        return detailedDrinks
    }

    fun searchCombined(query: String): List<Cocktail> {
        val nameResults = searchByName(query)
        val ingredientResults = searchByIngredient(query)
        val allResults = (nameResults + ingredientResults)
        return allResults.distinctBy { it.idDrink }
    }

    fun filterByCategory(category: String): List<Cocktail> {
        val c = URLEncoder.encode(category, "UTF-8")
        val url = BASE + "filter.php?c=$c"
        val json = get(url)
        return parseFilteredDrinks(json, category = category)
    }

    fun filterByAlcoholic(alcoholic: String): List<Cocktail> {
        val a = URLEncoder.encode(alcoholic, "UTF-8")
        val url = BASE + "filter.php?a=$a"
        val json = get(url)
        return parseFilteredDrinks(json)
    }

    fun getCategories(): List<String> {
        val url = BASE + "list.php?c=list"
        val json = get(url)
        val out = mutableListOf<String>()
        try {
            val obj = JSONObject(json)
            val arr: JSONArray? = obj.optJSONArray("drinks")
            arr?.let {
                for (i in 0 until it.length()) {
                    val item = it.getJSONObject(i)
                    val cat = item.optString("strCategory")
                    if (cat.isNotEmpty()) out += cat
                }
            }
        } catch (_: Exception) {}
        return out
    }

    fun getAlcoholicTypes(): List<String> {
        val url = BASE + "list.php?a=list"
        val json = get(url)
        val out = mutableListOf<String>()
        try {
            val obj = JSONObject(json)
            val arr: JSONArray? = obj.optJSONArray("drinks")
            arr?.let {
                for (i in 0 until it.length()) {
                    val item = it.getJSONObject(i)
                    val alcoholic = item.optString("strAlcoholic")
                    if (alcoholic.isNotEmpty()) out += alcoholic
                }
            }
        } catch (_: Exception) {}
        return out
    }

    fun getIngredients(): List<String> {
        val url = BASE + "list.php?i=list"
        val json = get(url)
        val out = mutableListOf<String>()
        try {
            val obj = JSONObject(json)
            val arr: JSONArray? = obj.optJSONArray("drinks")
            arr?.let {
                for (i in 0 until it.length()) {
                    val item = it.getJSONObject(i)
                    val ingredient = item.optString("strIngredient1")
                    if (ingredient.isNotEmpty()) out += ingredient
                }
            }
        } catch (_: Exception) {}
        return out
    }

    fun getRandomCocktail(): Cocktail? {
        val url = BASE + "random.php"
        val json = get(url)
        val cocktails = parseDetailedDrinks(json)
        return cocktails.firstOrNull()
    }

    // New method to lookup a cocktail by ID
    fun lookupById(id: String): Cocktail? {
        Log.d(TAG, "Looking up cocktail by ID: $id")
        val url = BASE + "lookup.php?i=$id"
        val json = get(url)
        Log.d(TAG, "Got JSON response for ID $id: ${json.take(100)}...")

        val cocktails = parseDetailedDrinks(json)

        if (cocktails.isNotEmpty()) {
            Log.d(TAG, "Found cocktail: ${cocktails[0].strDrink}")
            return cocktails[0]
        }

        Log.d(TAG, "No cocktail found with ID: $id")
        return null
    }

    private fun parseDetailedDrinks(json: String): List<Cocktail> {
        val out = mutableListOf<Cocktail>()
        try {
            val obj = JSONObject(json)
            val arr: JSONArray? = obj.optJSONArray("drinks")
            if (arr != null) {
                android.util.Log.d(TAG, "parseDetailedDrinks: found drinks array with ${arr.length()} items")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out += parseCocktailFromJson(o)
                }
            } else {
                android.util.Log.w(TAG, "parseDetailedDrinks: no drinks array found in JSON")
                android.util.Log.d(TAG, "parseDetailedDrinks: JSON content: ${json.take(200)}...")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "parseDetailedDrinks: error parsing JSON", e)
            android.util.Log.d(TAG, "parseDetailedDrinks: JSON content: ${json.take(200)}...")
        }
        return out
    }

    private fun parseFilteredDrinks(json: String, category: String? = null): List<Cocktail> {
        val out = mutableListOf<Cocktail>()
        try {
            val obj = JSONObject(json)
            val arr: JSONArray? = obj.optJSONArray("drinks")
            arr?.let {
                for (i in 0 until it.length()) {
                    val o = it.getJSONObject(i)
                    out += Cocktail(
                        idDrink = o.optString("idDrink"),
                        strDrink = o.optString("strDrink"),
                        strDrinkThumb = o.optString("strDrinkThumb"),
                        strCategory = category ?: o.optString("strCategory"),
                        strAlcoholic = o.optString("strAlcoholic")
                    )
                }
            }
        } catch (_: Exception) {}
        return out
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

    private fun get(urlStr: String): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
        }
        conn.inputStream.use { input ->
            BufferedReader(InputStreamReader(input)).use { br ->
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) sb.append(line)
                return sb.toString()
            }
        }
    }
}
