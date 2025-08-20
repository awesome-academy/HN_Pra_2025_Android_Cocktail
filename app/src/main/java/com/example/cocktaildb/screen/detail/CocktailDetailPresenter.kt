package com.example.cocktaildb.screen.detail

import com.example.cocktaildb.data.repository.CocktailRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CocktailDetailPresenter(
    private val repository: CocktailRepository
) : CocktailDetailContract.Presenter {

    companion object {
        private const val RELATED_COCKTAIL_LIMIT = 6
    }

    private var view: CocktailDetailContract.View? = null
    private val checkmarkService = CheckmarkFirebaseService()
    private val auth = FirebaseAuth.getInstance()
    private var presenterJob: Job? = null
    private val TAG = "CocktailDetailPresenter"

    override fun setView(view: CocktailDetailContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // Initialize if needed
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
                view?.showError("${e.message}")
            }
        }
    }

    override fun toggleBookmark(cocktail: Cocktail) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            view?.showError("Please sign in to bookmark cocktails")
            return
        }

        presenterJob?.cancel()
        presenterJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check current bookmark status
                val isBookmarked = withContext(Dispatchers.IO) {
                    checkmarkService.isCheckmarked(currentUser.uid, cocktail.idDrink)
                }

                if (isBookmarked.isSuccess) {
                    if (isBookmarked.getOrNull() == true) {
                        // Remove bookmark
                        val result = withContext(Dispatchers.IO) {
                            checkmarkService.removeCheckmark(currentUser.uid, cocktail.idDrink)
                        }

                        if (result.isSuccess) {
                            view?.updateBookmarkButtonState(false)
                        } else {
                            view?.showError("Failed to remove bookmark")
                        }
                    } else {
                        // Add bookmark
                        val result = withContext(Dispatchers.IO) {
                            checkmarkService.addCheckmark(currentUser.uid, cocktail.idDrink)
                        }

                        if (result.isSuccess) {
                            view?.updateBookmarkButtonState(true)
                        } else {
                            view?.showError("Failed to add bookmark")
                        }
                    }
                } else {
                    view?.showError("Failed to check bookmark status")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling bookmark", e)
                view?.showError("Error updating bookmark status")
            }
        }
    }

    override fun checkBookmarkStatus(cocktailId: String) {
        val currentUser = auth.currentUser
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
}

