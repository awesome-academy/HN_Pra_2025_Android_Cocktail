package com.example.cocktaildb.screen.home

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface HomeContract {
    interface View {
        fun showCocktails(cocktails: List<Cocktail>)
    }

    interface Presenter : BasePresenter<View> {
        fun loadCocktails()
    }
}

