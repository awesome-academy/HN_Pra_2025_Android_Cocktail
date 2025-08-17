package com.example.cocktaildb.screen.createrecipe

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface CreateRecipeContract {

    interface View {
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showSuccess(message: String)
        fun navigateToMyRecipes()
        fun addIngredientField()
        fun removeIngredientField(position: Int)
    }

    interface Presenter : BasePresenter<View> {
        fun saveRecipe(name: String, instructions: String, imageUrl: String,
                      ingredients: List<String>, measures: List<String>,
                      category: String, glass: String, alcoholic: Boolean)
        fun addIngredient()
        fun removeIngredient(position: Int)
    }
}
