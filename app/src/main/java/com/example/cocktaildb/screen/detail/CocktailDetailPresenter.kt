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
                    // Get cocktails by category first
                    val categoryCocktails = repository.filterByCategory(category)
                    
                    // Filter out the current cocktail and get up to 6 related ones
                    val relatedCocktails = categoryCocktails
                        .filter { it.strDrink != cocktailName }
                        .take(6)
                    
                    // If we don't have enough from category, try to get some random cocktails
                    if (relatedCocktails.size < 6) {
                        val allCocktails = repository.getCocktails()
                        val additionalCocktails = allCocktails
                            .filter { it.strDrink != cocktailName && !relatedCocktails.contains(it) }
                            .take(6 - relatedCocktails.size)
                        
                        (relatedCocktails + additionalCocktails).take(6)
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