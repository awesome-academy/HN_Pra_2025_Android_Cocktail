package com.example.cocktaildb.screen.detail

import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.utils.base.BasePresenter
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

    override fun setView(view: CocktailDetailContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // Initialize if needed
    }

    override fun onStop() {
        // Cleanup if needed
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
                view?.showError("Error loading related cocktails: ${e.message}")
            }
        }
    }
}

