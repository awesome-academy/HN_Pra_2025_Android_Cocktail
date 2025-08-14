package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.DataCocktail
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

    fun searchByName(query: String): List<DataCocktail> {
        val q = URLEncoder.encode(query, "UTF-8")
        val url = BASE + "search.php?s=$q"
        val json = get(url)
        return parseDetailedDrinks(json)
    }

    fun searchByIngredient(ingredient: String): List<DataCocktail> {
        val i = URLEncoder.encode(ingredient, "UTF-8")
        val url = BASE + "filter.php?i=$i"
        val json = get(url)
        val filteredDrinks = parseFilteredDrinks(json)
        val detailedDrinks = mutableListOf<DataCocktail>()
        for (drink in filteredDrinks) {
            val detailUrl = BASE + "lookup.php?i=${drink.id}"
            val detailJson = get(detailUrl)
            val detailedList = parseDetailedDrinks(detailJson)
            if (detailedList.isNotEmpty()) {
                detailedDrinks.add(detailedList[0])
            }
        }
        
        return detailedDrinks
    }

    fun searchCombined(query: String): List<DataCocktail> {
        val nameResults = searchByName(query)
        val ingredientResults = searchByIngredient(query)
        val allResults = (nameResults + ingredientResults)
        return allResults.distinctBy { it.id }
    }

    fun filterByCategory(category: String): List<DataCocktail> {
        val c = URLEncoder.encode(category, "UTF-8")
        val url = BASE + "filter.php?c=$c"
        val json = get(url)
        return parseFilteredDrinks(json, category = category)
    }

    fun filterByAlcoholic(alcoholic: String): List<DataCocktail> {
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

    private fun parseDetailedDrinks(json: String): List<DataCocktail> {
        val out = mutableListOf<DataCocktail>()
        try {
            val obj = JSONObject(json)
            val arr: JSONArray? = obj.optJSONArray("drinks")
            arr?.let {
                for (i in 0 until it.length()) {
                    val o = it.getJSONObject(i)
                    out += DataCocktail(
                        id = o.optString("idDrink"),
                        name = o.optString("strDrink"),
                        description = o.optString("strInstructions", null),
                        imageUrl = o.optString("strDrinkThumb", null),
                        category = o.optString("strCategory", null),
                        alcoholic = o.optString("strAlcoholic", null)
                    )
                }
            }
        } catch (_: Exception) {}
        return out
    }

    private fun parseFilteredDrinks(json: String, category: String? = null): List<DataCocktail> {
        val out = mutableListOf<DataCocktail>()
        try {
            val obj = JSONObject(json)
            val arr: JSONArray? = obj.optJSONArray("drinks")
            arr?.let {
                for (i in 0 until it.length()) {
                    val o = it.getJSONObject(i)
                    out += DataCocktail(
                        id = o.optString("idDrink"),
                        name = o.optString("strDrink"),
                        description = null,
                        imageUrl = o.optString("strDrinkThumb", null),
                        category = category ?: o.optString("strCategory", null),
                        alcoholic = o.optString("strAlcoholic", null)
                    )
                }
            }
        } catch (_: Exception) {}
        return out
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

