package com.example.cocktaildb.screen.myrecipe

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter


interface MyRecipeContract {


    interface View {

        fun showUserRecipes(cocktails: List<Cocktail>)


        fun displayLoading(show: Boolean)


        fun displayError(message: String)
    }


    interface Presenter : BasePresenter<View> {

        fun loadUserRecipes()
    }
}

