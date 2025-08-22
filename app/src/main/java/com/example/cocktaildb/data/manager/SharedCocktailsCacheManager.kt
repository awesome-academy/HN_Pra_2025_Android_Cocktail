package com.example.cocktaildb.data.manager

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.example.cocktaildb.data.model.Cocktail

class SharedCocktailsCacheManager(context: Context) {
	private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	fun saveCocktails(cocktails: List<Cocktail>) {
		val jsonArray = JSONArray()
		cocktails.forEach { c ->
			val obj = JSONObject().apply {
				put("idDrink", c.idDrink)
				put("strDrink", c.strDrink)
				put("strCategory", c.strCategory)
				put("strAlcoholic", c.strAlcoholic)
				put("strInstructions", c.strInstructions)
				put("strDrinkThumb", c.strDrinkThumb)
				put("ingredients", JSONArray(c.ingredients))
				put("measures", JSONArray(c.measures))
				if (c.rating != null) put("rating", c.rating) else put("rating", JSONObject.NULL)
			}
			jsonArray.put(obj)
		}
		prefs.edit().putString(KEY_SHARED_COCKTAILS, jsonArray.toString()).apply()
		prefs.edit().putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
	}

	fun loadCocktails(): List<Cocktail> {
		val json = prefs.getString(KEY_SHARED_COCKTAILS, null) ?: return emptyList()
		return try {
			val array = JSONArray(json)
			val list = mutableListOf<Cocktail>()
			for (i in 0 until array.length()) {
				val obj = array.getJSONObject(i)
				val ingredientsJson = obj.optJSONArray("ingredients") ?: JSONArray()
				val measuresJson = obj.optJSONArray("measures") ?: JSONArray()
				val ingredients = MutableList(ingredientsJson.length()) { idx -> ingredientsJson.optString(idx) }
				val measures = MutableList(measuresJson.length()) { idx -> measuresJson.optString(idx) }
				val rating = if (obj.isNull("rating")) null else obj.optDouble("rating").toFloat()
				list.add(
					Cocktail(
						idDrink = obj.optString("idDrink"),
						strDrink = obj.optString("strDrink"),
						strCategory = obj.optString("strCategory"),
						strAlcoholic = obj.optString("strAlcoholic"),
						strInstructions = obj.optString("strInstructions"),
						strDrinkThumb = obj.optString("strDrinkThumb"),
						ingredients = ingredients,
						measures = measures,
						rating = rating
					)
				)
			}
			list
		} catch (e: Exception) {
			emptyList()
		}
	}

	fun lastUpdatedAt(): Long = prefs.getLong(KEY_UPDATED_AT, 0L)

	companion object {
		private const val PREFS_NAME = "shared_cocktails_cache"
		private const val KEY_SHARED_COCKTAILS = "shared_cocktails"
		private const val KEY_UPDATED_AT = "updated_at"
	}
} 