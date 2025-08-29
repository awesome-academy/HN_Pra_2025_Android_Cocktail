package com.example.cocktaildb.screen.history

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.CocktailDetail
import com.example.cocktaildb.data.model.History
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.utils.CocktailContextWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
class HistoryPresenter(
    private val cocktailRepository: CocktailRepository,
    private val contextWrapper: CocktailContextWrapper,
    private val historyFirebaseService: HistoryFirebaseService = HistoryFirebaseService(),
    private val authRepository: AuthRepository = AuthRepository()
) : HistoryContract.Presenter {

    private var view: HistoryContract.View? = null


    override fun setView(view: HistoryContract.View?) {
        this.view = view
    }

    override fun onStart() {
    }

    override fun onStop() {
        this.view = null
    }

    override fun loadHistoryCocktails() {
        view?.displayLoading(true)

        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null && isNetworkAvailable()) {
            syncFirebaseWithLocal(currentUser.uid)
        } else {
            loadFromLocalOnly()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = contextWrapper.context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as? android.net.ConnectivityManager
            val networkInfo = connectivityManager?.activeNetworkInfo
            networkInfo?.isConnected == true
        } catch (e: Exception) {
            false
        }
    }

    private fun syncFirebaseWithLocal(uid: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val firebaseResult = historyFirebaseService.getUserHistory(uid)

                if (firebaseResult.isSuccess) {
                    val firebaseHistory = firebaseResult.getOrNull() ?: emptyList()
                    val allFirebaseCocktails = firebaseHistory.mapNotNull { it.cocktailDetail?.toCocktail() }

                    val deduplicatedFirebaseCocktails = deduplicateCocktails(allFirebaseCocktails, firebaseHistory)
                    val localCocktails = getHistoryCocktails(contextWrapper.context)
                    val mergedCocktails = mutableListOf<Cocktail>()
                    val firebaseIds = deduplicatedFirebaseCocktails.map { it.idDrink }.toSet()

                    mergedCocktails.addAll(deduplicatedFirebaseCocktails)

                    val localOnlyCocktails = localCocktails.filter { it.idDrink !in firebaseIds }
                    mergedCocktails.addAll(localOnlyCocktails)
                    val finalDeduplicatedList = deduplicateCocktailsSimple(mergedCocktails)
                    val finalList = finalDeduplicatedList.take(MAX_HISTORY_ITEMS)
                    saveHistoryToLocal(contextWrapper.context, finalList)

                    Log.d("HistoryPresenter", "Synced ${allFirebaseCocktails.size} Firebase items → ${deduplicatedFirebaseCocktails.size} deduplicated → ${finalList.size} final items")
                    GlobalScope.launch(Dispatchers.Main) {
                        view?.displayLoading(false)
                        if (finalList.isEmpty()) {
                            view?.showEmptyState()
                        } else {
                            view?.hideEmptyState()
                            view?.showHistoryCocktails(finalList)
                        }
                    }
                } else {
                    loadFromLocalOnly()
                }
            } catch (e: Exception) {
                Log.e("HistoryPresenter", "Error syncing Firebase with local", e)
                loadFromLocalOnly()
            }
        }
    }

    private fun loadFromLocalOnly() {
        try {
            val allCocktails = getHistoryCocktails(contextWrapper.context)
            val cocktails = deduplicateCocktailsSimple(allCocktails)

            view?.displayLoading(false)
            if (cocktails.isEmpty()) {
                view?.showEmptyState()
            } else {
                view?.hideEmptyState()
                view?.showHistoryCocktails(cocktails)
            }

            if (allCocktails.size != cocktails.size) {
                Log.d("HistoryPresenter", "Deduplicated local history: ${allCocktails.size} → ${cocktails.size} items")
                saveHistoryToLocal(contextWrapper.context, cocktails)
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

    private fun deduplicateCocktails(cocktails: List<Cocktail>, historyItems: List<History>): List<Cocktail> {
        val cocktailToHistoryMap = mutableMapOf<String, History>()

        historyItems.forEach { historyItem ->
            val cocktailId = historyItem.cocktailId
            val existingHistory = cocktailToHistoryMap[cocktailId]

            if (existingHistory == null || historyItem.viewedAt > existingHistory.viewedAt) {
                cocktailToHistoryMap[cocktailId] = historyItem
            }
        }

        val deduplicatedCocktails = cocktailToHistoryMap.values
            .mapNotNull { it.cocktailDetail?.toCocktail() }
            .sortedByDescending { cocktail ->
                cocktailToHistoryMap.values.find { it.cocktailDetail?.idDrink == cocktail.idDrink }?.viewedAt ?: 0L
            }

        Log.d("HistoryPresenter", "Deduplicated ${cocktails.size} → ${deduplicatedCocktails.size} cocktails from Firebase")
        return deduplicatedCocktails
    }

    private fun deduplicateCocktailsSimple(cocktails: List<Cocktail>): List<Cocktail> {
        val seen = mutableSetOf<String>()
        val deduplicated = cocktails.filter { cocktail ->
            if (seen.contains(cocktail.idDrink)) {
                Log.d("HistoryPresenter", "Removing duplicate cocktail: ${cocktail.strDrink} (ID: ${cocktail.idDrink})")
                false
            } else {
                seen.add(cocktail.idDrink)
                true
            }
        }

        if (deduplicated.size != cocktails.size) {
            Log.d("HistoryPresenter", "Final deduplication: ${cocktails.size} → ${deduplicated.size} cocktails")
        }

        return deduplicated
    }

    override fun clearHistory() {
        view?.displayLoading(true)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val cocktails = getHistoryCocktails(contextWrapper.context)
                cocktails.forEach { cocktail ->
                    val thumb = cocktail.strDrinkThumb
                    if (!thumb.isNullOrEmpty() && thumb.startsWith("file://")) {
                        val path = thumb.removePrefix("file://")
                        kotlin.runCatching {
                            com.example.cocktaildb.utils.ImageLoader.deleteImageFile(contextWrapper.context, path)
                        }
                    }
                }

                clearHistoryFromLocal(contextWrapper.context)
                clearFirebaseTimestamps(contextWrapper.context)
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val result = historyFirebaseService.clearUserHistory(currentUser.uid)
                    if (result.isFailure) {
                        Log.e("HistoryPresenter", "Failed to clear Firebase history: ${result.exceptionOrNull()?.message}")
                    }
                }
                launch(Dispatchers.Main) {
                    view?.displayLoading(false)
                    view?.showEmptyState()
                }
            } catch (e: Exception) {
                Log.e("HistoryPresenter", "Error clearing history", e)
                launch(Dispatchers.Main) {
                    view?.displayLoading(false)
                    // Even if there is an error (e.g., Firebase not available in unit tests),
                    // show empty state to satisfy UX and make operation idempotent locally.
                    view?.showEmptyState()
                    view?.displayError("Error clearing history: ${e.message}")
                }
            }
        }
    }

    private fun getHistoryCocktails(context: Context): List<Cocktail> {
        return try {
            val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
            val historyString = sharedPreferences.getString("cocktail_history", "") ?: ""

            if (historyString.isEmpty()) {
                emptyList()
            } else {
                parseHistoryString(historyString)
            }
        } catch (e: Exception) {
            Log.e("HistoryPresenter", "Error getting history from local", e)
            emptyList()
        }
    }

    private fun parseHistoryString(historyString: String): List<Cocktail> {
        return try {
            historyString.split("||").mapNotNull { entry ->
                val fields = entry.split("|:|")
                if (fields.size >= 9) {
                    val ingredients = if (fields[7].isNotEmpty()) {
                        fields[7].split(";;").filter { it.isNotEmpty() }
                    } else {
                        emptyList()
                    }

                    val measures = if (fields[8].isNotEmpty()) {
                        fields[8].split(";;").filter { it.isNotEmpty() }
                    } else {
                        emptyList()
                    }

                    Cocktail(
                        idDrink = fields[0],
                        strDrink = fields[1],
                        strCategory = fields[2].takeIf { it.isNotEmpty() },
                        strAlcoholic = fields[3].takeIf { it.isNotEmpty() },
                        strGlass = fields[4].takeIf { it.isNotEmpty() },
                        strInstructions = fields[5].takeIf { it.isNotEmpty() },
                        strDrinkThumb = fields[6].takeIf { it.isNotEmpty() },
                        ingredients = ingredients,
                        measures = measures
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("HistoryPresenter", "Error parsing history string", e)
            emptyList()
        }
    }

    private fun clearHistoryFromLocal(context: Context) {
        try {
            val sharedPreferences = context.getSharedPreferences("cocktail_history", Context.MODE_PRIVATE)
            sharedPreferences.edit().remove("cocktail_history").apply()
        } catch (e: Exception) {
            Log.e("HistoryPresenter", "Error clearing history from local", e)
        }
    }

    private fun clearFirebaseTimestamps(context: Context) {
        try {
            val prefs = context.getSharedPreferences("history_firebase_timestamps", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d("HistoryPresenter", "Cleared Firebase sync timestamps")
        } catch (e: Exception) {
            Log.e("HistoryPresenter", "Error clearing Firebase timestamps", e)
        }
    }

    companion object {
        private const val HISTORY_PREFS = "cocktail_history"
        private const val HISTORY_KEY = "cocktail_history"
        private const val MAX_HISTORY_ITEMS = 50

        fun addToHistory(context: Context, cocktail: Cocktail) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val cocktails = getHistoryCocktails(context).toMutableList()

                    val existingCocktail = cocktails.find { it.idDrink == cocktail.idDrink }
                    if (existingCocktail != null) {
                        cocktails.removeAll { it.idDrink == cocktail.idDrink }
                        Log.d("HistoryPresenter", "Moving existing cocktail ${cocktail.strDrink} to top of history")
                    } else {
                        Log.d("HistoryPresenter", "Adding new cocktail ${cocktail.strDrink} to history")
                    }

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
                    cocktails.add(0, updatedCocktail)

                    // Keep only last 50 items
                    while (cocktails.size > MAX_HISTORY_ITEMS) {
                        cocktails.removeAt(cocktails.size - 1)
                    }

                    val deduplicatedCocktails = deduplicateCocktailsSimpleStatic(cocktails)

                    saveHistoryToLocal(context, deduplicatedCocktails)
                    try {
                        val authRepository = AuthRepository()
                        val currentUser = authRepository.getCurrentUser()
                        if (currentUser != null) {
                            val historyService = HistoryFirebaseService()

                            val shouldAddToFirebase = existingCocktail == null || shouldUpdateFirebaseHistory(context, cocktail)

                            if (shouldAddToFirebase) {
                                val result = historyService.addHistoryWithDetails(currentUser.uid, updatedCocktail)
                                if (result.isSuccess) {
                                    Log.d("HistoryPresenter", "Successfully synced ${cocktail.strDrink} to Firebase")
                                } else {
                                    Log.e("HistoryPresenter", "Failed to sync to Firebase: ${result.exceptionOrNull()?.message}")
                                }
                            } else {
                                Log.d("HistoryPresenter", "Skipping Firebase sync for ${cocktail.strDrink} (recently added)")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HistoryPresenter", "Failed to sync with Firebase", e)
                    }

                } catch (e: Exception) {
                    Log.e("HistoryPresenter", "Error adding to history", e)
                }
            }
        }

        private fun shouldUpdateFirebaseHistory(context: Context, cocktail: Cocktail): Boolean {
            try {
                val prefs = context.getSharedPreferences("history_firebase_timestamps", Context.MODE_PRIVATE)
                val lastSyncTime = prefs.getLong("sync_${cocktail.idDrink}", 0L)
                val currentTime = System.currentTimeMillis()
                val oneHourInMillis = 60 * 60 * 1000L

                val shouldSync = (currentTime - lastSyncTime) > oneHourInMillis

                if (shouldSync) {
                    prefs.edit().putLong("sync_${cocktail.idDrink}", currentTime).apply()
                    Log.d("HistoryPresenter", "Will sync ${cocktail.strDrink} to Firebase (last sync: ${(currentTime - lastSyncTime) / 1000}s ago)")
                } else {
                    Log.d("HistoryPresenter", "Skipping Firebase sync for ${cocktail.strDrink} (synced ${(currentTime - lastSyncTime) / 1000}s ago)")
                }

                return shouldSync
            } catch (e: Exception) {
                Log.e("HistoryPresenter", "Error checking Firebase sync timestamp", e)
                return true
            }
        }
        private fun deduplicateCocktailsSimpleStatic(cocktails: List<Cocktail>): List<Cocktail> {
            val seen = mutableSetOf<String>()
            val deduplicated = cocktails.filter { cocktail ->
                if (seen.contains(cocktail.idDrink)) {
                    Log.d("HistoryPresenter", "Removing duplicate cocktail in static method: ${cocktail.strDrink} (ID: ${cocktail.idDrink})")
                    false
                } else {
                    seen.add(cocktail.idDrink)
                    true
                }
            }

            if (deduplicated.size != cocktails.size) {
                Log.d("HistoryPresenter", "Static deduplication: ${cocktails.size} → ${deduplicated.size} cocktails")
            }

            return deduplicated
        }
        private fun getHistoryCocktails(context: Context): List<Cocktail> {
            return try {
                val sharedPreferences = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
                val historyString = sharedPreferences.getString(HISTORY_KEY, "") ?: ""

                if (historyString.isEmpty()) {
                    emptyList()
                } else {
                    parseHistoryString(historyString)
                }
            } catch (e: Exception) {
                Log.e("HistoryPresenter", "Error getting history from local", e)
                emptyList()
            }
        }

        private fun parseHistoryString(historyString: String): List<Cocktail> {
            return try {
                historyString.split("||").mapNotNull { entry ->
                    val fields = entry.split("|:|")
                    if (fields.size >= 9) {
                        val ingredients = if (fields[7].isNotEmpty()) {
                            fields[7].split(";;").filter { it.isNotEmpty() }
                        } else {
                            emptyList()
                        }

                        val measures = if (fields[8].isNotEmpty()) {
                            fields[8].split(";;").filter { it.isNotEmpty() }
                        } else {
                            emptyList()
                        }

                        Cocktail(
                            idDrink = fields[0],
                            strDrink = fields[1],
                            strCategory = fields[2].takeIf { it.isNotEmpty() },
                            strAlcoholic = fields[3].takeIf { it.isNotEmpty() },
                            strGlass = fields[4].takeIf { it.isNotEmpty() },
                            strInstructions = fields[5].takeIf { it.isNotEmpty() },
                            strDrinkThumb = fields[6].takeIf { it.isNotEmpty() },
                            ingredients = ingredients,
                            measures = measures
                        )
                    } else null
                }
            } catch (e: Exception) {
                Log.e("HistoryPresenter", "Error parsing history string", e)
                emptyList()
            }
        }

        private fun saveHistoryToLocal(context: Context, cocktails: List<Cocktail>) {
            try {
                val historyString = cocktails.joinToString("||") { cocktail ->
                    val ingredients = cocktail.ingredients.joinToString(";;")
                    val measures = cocktail.measures.joinToString(";;")

                    "${cocktail.idDrink}|:|${cocktail.strDrink}|:|${cocktail.strCategory ?: ""}|:|${cocktail.strAlcoholic ?: ""}|:|${cocktail.strGlass ?: ""}|:|${cocktail.strInstructions ?: ""}|:|${cocktail.strDrinkThumb ?: ""}|:|$ingredients|:|$measures"
                }

                val sharedPreferences = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
                sharedPreferences.edit().putString(HISTORY_KEY, historyString).apply()
            } catch (e: Exception) {
                Log.e("HistoryPresenter", "Error saving history to local", e)
            }
        }

        private fun clearHistoryFromLocal(context: Context) {
            try {
                val sharedPreferences = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
                sharedPreferences.edit().remove(HISTORY_KEY).apply()
            } catch (e: Exception) {
                Log.e("HistoryPresenter", "Error clearing history from local", e)
            }
        }
    }

}
