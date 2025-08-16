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

object CocktailService {
    private const val BASE = "https://www.thecocktaildb.com/api/json/v1/1/"

    fun searchByName(query: String): List<Cocktail> {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = BASE + "search.php?s=$q"
        val json = get(url)
        return parseDetailedDrinks(json)
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

    private fun parseDetailedDrinks(json: String): List<Cocktail> {
        val out = mutableListOf<Cocktail>()
        try {
            val obj = JSONObject(json)
            val arr: JSONArray? = obj.optJSONArray("drinks")
            arr?.let {
                for (i in 0 until it.length()) {
                    val o = it.getJSONObject(i)
                    out += parseCocktailFromJson(o)
                }
            }
        } catch (_: Exception) {}
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

