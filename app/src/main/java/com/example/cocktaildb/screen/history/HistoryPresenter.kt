package com.example.cocktaildb.screen.history

import android.content.Context
import android.content.SharedPreferences
import com.example.cocktaildb.data.model.Cocktail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
class HistoryPresenter(
    private val context: Context
) : HistoryContract.Presenter {

    private var view: HistoryContract.View? = null
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)

    override fun setView(view: HistoryContract.View?) {
        this.view = view
        if (view != null) {
            loadHistoryCocktails()
        }
    }

    override fun onStart() {
        // Refresh data when view starts
        loadHistoryCocktails()
    }

    override fun onStop() {
        this.view = null
    }

    override fun loadHistoryCocktails() {
        view?.displayLoading(true)

        try {
            val historyString = sharedPreferences.getString("cocktail_history", "") ?: ""
            val cocktails: List<Cocktail> = if (historyString.isEmpty()) {
                emptyList()
            } else {
                historyString.split("||").mapNotNull { entry ->
                    val fields = entry.split("|:|")
                    if (fields.size >= 3) {
                        Cocktail(idDrink = fields[0], strDrink = fields[1], strDrinkThumb = fields[2])
                    } else null
                }
            }

            view?.displayLoading(false)

            if (cocktails.isEmpty()) {
                view?.showEmptyState()
            } else {
                view?.hideEmptyState()
                view?.showHistoryCocktails(cocktails)
            }
        } catch (e: Exception) {
            view?.displayLoading(false)
            view?.displayError("Error loading history: ${e.message}")
            view?.showEmptyState()
        }
    }

    override fun onCocktailClicked(cocktail: Cocktail) {
        view?.navigateToCocktailDetail(cocktail)
    }

    override fun clearHistory() {
        try {
            // Delete any locally cached images associated with history items
            val historyString = sharedPreferences.getString("cocktail_history", "") ?: ""
            val historyCocktails: List<Cocktail> = if (historyString.isEmpty()) {
                emptyList()
            } else {
                historyString.split("||").mapNotNull { entry ->
                    val fields = entry.split("|:|")
                    if (fields.size >= 2) {
                        Cocktail(idDrink = fields[0], strDrink = fields[1])
                    } else null
                }
            }
            historyCocktails.forEach { c ->
                val thumb = c.strDrinkThumb
                if (!thumb.isNullOrEmpty() && thumb.startsWith("file://")) {
                    val path = thumb.removePrefix("file://")
                    kotlin.runCatching {
                        com.example.cocktaildb.utils.ImageLoader.deleteImageFile(context, path)
                    }
                }
            }

            sharedPreferences.edit().remove("cocktail_history").apply()
            view?.showEmptyState()
        } catch (e: Exception) {
            view?.displayError("Error clearing history: ${e.message}")
        }
    }

    companion object {
        fun addToHistory(context: Context, cocktail: Cocktail) {
            GlobalScope.launch(Dispatchers.IO) {
                val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)

                try {
                    val historyString = sharedPreferences.getString("cocktail_history", "") ?: ""
                    val historyCocktails: MutableList<Cocktail> = if (historyString.isEmpty()) {
                        mutableListOf()
                    } else {
                        historyString.split("||").mapNotNull { entry ->
                            val fields = entry.split("|:|")
                            if (fields.size >= 3) {
                                Cocktail(idDrink = fields[0], strDrink = fields[1], strDrinkThumb = fields[2])
                            } else null
                        }.toMutableList()
                    }

                    // Remove if already exists to avoid duplicates
                    historyCocktails.removeAll { it.idDrink == cocktail.idDrink }

                    // Try to cache image locally for offline use
                    val updatedThumb = kotlin.runCatching {
                        val url = cocktail.strDrinkThumb
                        if (!url.isNullOrEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                            com.example.cocktaildb.utils.ImageLoader.saveImageFromUrlToInternalStorage(context, url)?.let { path ->
                                com.example.cocktaildb.utils.ImageLoader.getFileUri(path)
                            }
                        } else {
                            url
                        }
                    }.getOrNull() ?: cocktail.strDrinkThumb

                    val updatedCocktail = cocktail.copy(strDrinkThumb = updatedThumb)

                    // Add to beginning of list
                    historyCocktails.add(0, updatedCocktail)

                    // Keep only last 50 items
                    while (historyCocktails.size > 50) {
                        historyCocktails.removeAt(historyCocktails.size - 1)
                    }

                    val newHistoryString = historyCocktails.joinToString("||") { "${it.idDrink}|:|${it.strDrink}|:|${it.strDrinkThumb ?: ""}" }
                    sharedPreferences.edit().putString("cocktail_history", newHistoryString).apply()
                } catch (_: Exception) {
                    // Handle error silently
                }
            }
        }
    }

}