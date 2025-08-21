package com.example.cocktaildb.screen.myrecipe

import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.utils.base.BasePresenter


interface MyRecipeContract {


    interface View {

        fun showUserRecipes(recipes: List<Recipe>)


        fun displayLoading(show: Boolean)


        fun displayError(message: String)
    }


    interface Presenter : BasePresenter<View> {

        fun loadUserRecipes()
    }
}

