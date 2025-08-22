package com.example.cocktaildb.screen.shared

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.example.cocktaildb.data.manager.SharedCocktailsCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

class AllSharedCocktailsPresenter(
    private val recipeFirebaseService: RecipeFirebaseService
) : AllSharedCocktailsContract.Presenter {

    companion object {
        private const val TAG = "AllSharedCocktailsPresenter"
        private const val CACHE_STALE_MS = 10 * 60 * 1000L // 10 minutes
    }

    private var view: AllSharedCocktailsContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())

    // Cache in memory
    private var cachedCocktails: List<Cocktail>? = null
    private var isLoading = false

    private var cacheManager: SharedCocktailsCacheManager? = null

    override fun setView(view: AllSharedCocktailsContract.View?) {
        this.view = view
        if (view is com.example.cocktaildb.utils.base.BaseFragment<*>) {
            val ctx = (view as com.example.cocktaildb.utils.base.BaseFragment<*>).requireContext()
            cacheManager = SharedCocktailsCacheManager(ctx)
        }
    }

    override fun onStart() { }

    override fun onStop() {
        presenterScope.cancel()
        view = null
    }

    override fun loadAllSharedRecipes() {
        if (isLoading) return

        // 1) Show memory cache if any
        cachedCocktails?.let {
            view?.showAllSharedRecipes(it)
        }

        // 2) Show disk cache immediately if available
        val diskCache = cacheManager?.loadCocktails().orEmpty()
        if (diskCache.isNotEmpty() && cachedCocktails == null) {
            Log.d(TAG, "Show disk-cached cocktails: ${diskCache.size}")
            cachedCocktails = diskCache
            view?.showAllSharedRecipes(diskCache)
        }

        // 3) Decide to refresh from network if stale or empty
        val lastUpdated = cacheManager?.lastUpdatedAt() ?: 0L
        val isStale = System.currentTimeMillis() - lastUpdated > CACHE_STALE_MS
        if (cachedCocktails.isNullOrEmpty() || isStale) {
            fetchFromNetworkAndCache()
        }
    }

    private fun fetchFromNetworkAndCache() {
        isLoading = true
        view?.displayLoading(true)
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    recipeFirebaseService.getAllSharedRecipes()
                }
                result.fold(
                    onSuccess = { cocktails ->
                        cachedCocktails = cocktails
                        cacheManager?.saveCocktails(cocktails)
                        view?.showAllSharedRecipes(cocktails)
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Load shared cocktails failed: ${e.message}")
                        if (cachedCocktails.isNullOrEmpty()) {
                            view?.displayError(e.message ?: "Failed to load shared cocktails")
                        }
                    }
                )
            } finally {
                isLoading = false
                view?.displayLoading(false)
            }
        }
    }

    override fun refreshAllSharedRecipes() {
        fetchFromNetworkAndCache()
    }

    fun clearCache() {
        cachedCocktails = null
    }
}

