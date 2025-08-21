package com.example.cocktaildb.screen.home

import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.base.BasePresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

class HomePresenter(
    private val repository: CocktailRepository
) : HomeContract.Presenter {

    private var view: HomeContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())

    override fun setView(view: HomeContract.View?) {
        this.view = view
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
