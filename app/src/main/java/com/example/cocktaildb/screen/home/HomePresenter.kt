package com.example.cocktaildb.screen.home

import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.base.BasePresenter

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
        try {
            val cocktails = repository.getCocktails()
            view?.showCocktails(cocktails)
        } catch (e: Exception) {
            (view as? BaseFragment<*>)?.showError(e.message ?: "Unknown error")
        } finally {
            (view as? BaseFragment<*>)?.hideLoading()
        }
    }
}
