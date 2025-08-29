package com.example.cocktaildb.screen.detail

import android.content.Context
import android.util.Log
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.CheckmarkFirebaseService
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.data.manager.FavoritesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CocktailDetailPresenter(
    private val repository: CocktailRepository,
    private val authRepository: AuthRepository,
    private val checkmarkService: CheckmarkFirebaseService = CheckmarkFirebaseService(),
    private val historyFirebaseService: HistoryFirebaseService = HistoryFirebaseService()
) : CocktailDetailContract.Presenter {
    companion object {
        private const val RELATED_COCKTAIL_LIMIT = 6
    }

    private var view: CocktailDetailContract.View? = null
    private var presenterJob: Job? = null
    private val TAG = "CocktailDetailPresenter"

    override fun setView(view: CocktailDetailContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // Initialize favorites manager if needed
        if (!FavoritesManager.isInitialized()) {
            FavoritesManager.loadFavoritesFromFirestore { success ->
                if (!success) {
                    Log.e(TAG, "Failed to load favorites")
                }
            }
        }
    }

    override fun onStop() {
        presenterJob?.cancel()
        presenterJob = null
        view = null
    }

    override fun loadRelatedCocktails(cocktailName: String, category: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val relatedCocktails = withContext(Dispatchers.IO) {
                    val categoryCocktails = repository.filterByCategory(category)
                    val relatedCocktails = categoryCocktails
                        .filter { it.strDrink != cocktailName }
                        .take(RELATED_COCKTAIL_LIMIT)
                    if (relatedCocktails.size < RELATED_COCKTAIL_LIMIT) {
                        val allCocktails = repository.getCocktails()
                        val additionalCocktails = allCocktails
                            .filter { it.strDrink != cocktailName && !relatedCocktails.contains(it) }
                            .take(RELATED_COCKTAIL_LIMIT - relatedCocktails.size)
                        (relatedCocktails + additionalCocktails).take(RELATED_COCKTAIL_LIMIT)
                    } else {
                        relatedCocktails
                    }
                }
                view?.showRelatedCocktails(relatedCocktails)
            } catch (e: Exception) {
            }
        }
    }

    override fun toggleBookmark(cocktail: Cocktail) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            view?.showErrorResource(R.string.error_please_sign_in_to_bookmark)
            return
        }
        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val isBookmarked = withContext(Dispatchers.IO) {
                    checkmarkService.isCheckmarked(currentUser.uid, cocktail.idDrink)
                }
                if (isBookmarked.isSuccess) {
                    if (isBookmarked.getOrNull() == true) {
                        val result = withContext(Dispatchers.IO) {
                            checkmarkService.removeCheckmark(currentUser.uid, cocktail.idDrink)
                        }
                        if (result.isSuccess) {
                            view?.updateBookmarkButtonState(false)
                        } else {
                            view?.showErrorResource(R.string.error_failed_to_remove_bookmark)
                        }
                    } else {
                        val result = withContext(Dispatchers.IO) {
                            checkmarkService.addCheckmark(currentUser.uid, cocktail.idDrink)
                        }
                        if (result.isSuccess) {
                            view?.updateBookmarkButtonState(true)
                        } else {
                            view?.showErrorResource(R.string.error_failed_to_add_bookmark)
                        }
                    }
                } else {
                    view?.showErrorResource(R.string.error_failed_to_check_bookmark_status)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling bookmark", e)
                view?.showErrorResource(R.string.error_updating_bookmark_status)
            }
        }
    }

    override fun checkBookmarkStatus(cocktailId: String) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            view?.updateBookmarkButtonState(false)
            return
        }
        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val isBookmarked = withContext(Dispatchers.IO) {
                    checkmarkService.isCheckmarked(currentUser.uid, cocktailId)
                }
                if (isBookmarked.isSuccess) {
                    view?.updateBookmarkButtonState(isBookmarked.getOrNull() == true)
                } else {
                    view?.updateBookmarkButtonState(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking bookmark status", e)
                view?.updateBookmarkButtonState(false)
            }
        }
    }

    override fun checkFavoriteStatus(cocktailId: String) {
        val isFavorite = FavoritesManager.isFavorite(cocktailId)
        view?.updateFavoriteButtonState(isFavorite)
    }

    override fun toggleFavorite(cocktail: Cocktail) {
        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val isFavorite = withContext(Dispatchers.IO) {
                    FavoritesManager.toggleFavorite(cocktail)
                }
                view?.updateFavoriteButtonState(isFavorite)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite", e)
                view?.showErrorResource(R.string.error_updating_favorite_status)
            }
        }
    }

}
