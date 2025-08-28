package com.example.cocktaildb.screen.home

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.model.CocktailTable
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.example.cocktaildb.data.manager.SharedCocktailsCacheManager
import com.example.cocktaildb.utils.base.BaseFragment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

class HomePresenter(
    private val cocktailRepository: CocktailRepository,
    private val authRepository: AuthRepository,
    private val historyFirebaseService: HistoryFirebaseService,
    private val recipeFirebaseService: RecipeFirebaseService,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HomeContract.Presenter {

    private var view: HomeContract.View? = null
    private val presenterScope = CoroutineScope(mainDispatcher + Job())

    private var sharedCacheManager: SharedCocktailsCacheManager? = null
    private var cachedSharedCocktails: List<Cocktail>? = null
    private var isLoadingShared = false

    override fun setView(view: HomeContract.View?) {
        this.view = view
        if (view is BaseFragment<*>) {
            val ctx = (view as BaseFragment<*>).requireContext()
            sharedCacheManager = SharedCocktailsCacheManager(ctx)
        }
    }

    override fun onStart() {
        // No initialization needed
    }

    override fun onStop() {
        presenterScope.cancel() // Cancel all coroutines when stopping
        view = null
    }

    override fun loadCocktails() {
        (view as? BaseFragment<*>)?.showLoading()

        presenterScope.launch {
            try {
                val cocktails = withContext(ioDispatcher) {
                    cocktailRepository.fetchCocktailsFromApi()
                }
                view?.showCocktails(cocktails)
            } catch (e: Exception) {
                (view as? BaseFragment<*>)?.showError(e.message ?: "Unknown error")
            } finally {
                (view as? BaseFragment<*>)?.hideLoading()
            }
        }
    }

    override fun loadSharedCocktails() {
        if (isLoadingShared) return
        (view as? BaseFragment<*>)?.showLoading()

        // 1) Show memory cache if available
        cachedSharedCocktails?.let { list ->
            view?.showSharedCocktails(list)
        }

        // 2) Show disk cache immediately if memory empty
        if (cachedSharedCocktails == null) {
            val disk = sharedCacheManager?.loadCocktails().orEmpty()
            if (disk.isNotEmpty()) {
                cachedSharedCocktails = disk
                view?.showSharedCocktails(disk)
            }
        }

        // 3) Refresh from network
        isLoadingShared = true
        presenterScope.launch {
            try {
                val shared = withContext(ioDispatcher) {
                    recipeFirebaseService.getSharedRecipes(limit = 20)
                }
                shared.onSuccess { list ->
                    cachedSharedCocktails = list
                    sharedCacheManager?.saveCocktails(list)
                    view?.showSharedCocktails(list)
                }.onFailure { err ->
                    (view as? BaseFragment<*>)?.showError(err.message ?: "Load shared failed")
                }
            } catch (e: Exception) {
                (view as? BaseFragment<*>)?.showError(e.message ?: "Unknown error")
            } finally {
                isLoadingShared = false
                (view as? BaseFragment<*>)?.hideLoading()
            }
        }
    }

    override fun onCocktailClicked(cocktail: Cocktail) {
        view?.navigateToCocktailDetail(cocktail)
    }
}

private fun CocktailTable.toUiCocktail(): Cocktail =
    Cocktail(
        idDrink = id,
        strDrink = name,
        strCategory = category,
        strAlcoholic = alcoholic,
        strInstructions = instructions,
        strDrinkThumb = thumbnailUrl,
        ingredients = ingredients
    )
