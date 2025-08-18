package com.example.cocktaildb.screen.detail

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface CocktailDetailContract {
    interface View {
        fun showRelatedCocktails(cocktails: List<Cocktail>)
        fun showError(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadRelatedCocktails(cocktailName: String, category: String)
    }
} 