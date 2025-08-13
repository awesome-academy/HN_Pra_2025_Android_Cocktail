package com.example.cocktaildb.screen.home

import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.base.BasePresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomePresenter(
    private val repository: CocktailRepository
) : HomeContract.Presenter {

    private var view: HomeContract.View? = null

    override fun setView(view: HomeContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // TODO: Initialize if needed
    }

    override fun onStop() {
        // TODO: Cleanup if needed
    }

    override fun loadCocktails() {
        (view as? BaseFragment<*>)?.showLoading()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cocktails = withContext(Dispatchers.IO) {
                    repository.fetchCocktailsFromApi()
                }
                view?.showCocktails(cocktails)
            } catch (e: Exception) {
                (view as? BaseFragment<*>)?.showError(e.message ?: "Unknown error")
            } finally {
                (view as? BaseFragment<*>)?.hideLoading()
            }
        }
    }
}

