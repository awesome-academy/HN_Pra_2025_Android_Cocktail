package com.example.cocktaildb.screen.shared

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface AllSharedCocktailsContract {

    interface View {
        fun showAllSharedRecipes(cocktails: List<Cocktail>)
        fun displayLoading(show: Boolean)
        fun displayError(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadAllSharedRecipes()
        fun refreshAllSharedRecipes()
    }
}

