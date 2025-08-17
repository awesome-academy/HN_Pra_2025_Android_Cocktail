package com.example.cocktaildb.screen.createrecipe

import android.util.Log
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.utils.base.BasePresenter
import java.util.UUID

class CreateRecipePresenter(
    private val cocktailRepository: CocktailRepository,
    private val authRepository: AuthRepository
) : CreateRecipeContract.Presenter {

    private var view: CreateRecipeContract.View? = null

    override fun setView(view: CreateRecipeContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // Initialize any resources when presenter starts
    }

    override fun onStop() {
        // Clean up resources when presenter stops
    }

    override fun saveRecipe(
        name: String,
        instructions: String,
        imageUrl: String,
        ingredients: List<String>,
        measures: List<String>,
        category: String,
        glass: String,
        alcoholic: Boolean
    ) {
        if (name.isBlank()) {
            view?.showError("Recipe name cannot be empty")
            return
        }

        if (instructions.isBlank()) {
            view?.showError("Instructions cannot be empty")
            return
        }

        if (ingredients.isEmpty() || ingredients.any { it.isBlank() }) {
            view?.showError("Please add at least one ingredient")
            return
        }

        view?.showLoading(true)

        try {
            // Create a new cocktail object
            val currentUser = authRepository.getCurrentUser()
            val userId = currentUser?.uid ?: "unknown_user"

            // Create cocktail based on your app's existing Cocktail model
            val cocktail = Cocktail(
                idDrink = UUID.randomUUID().toString(),
                strDrink = name,
                strCategory = category,
                strAlcoholic = if (alcoholic) "Alcoholic" else "Non alcoholic",
                strGlass = glass,
                strInstructions = instructions,
                strDrinkThumb = imageUrl,
                ingredients = ingredients.filter { it.isNotBlank() },
                measures = measures.filter { it.isNotBlank() }
            )

            // In a real implementation, this would save to the repository
            // For now we'll just simulate success
            Log.d("CreateRecipePresenter", "Created recipe: $cocktail")

            view?.showLoading(false)
            view?.showSuccess("Recipe saved successfully!")
            view?.navigateToMyRecipes()

        } catch (e: Exception) {
            view?.showLoading(false)
            view?.showError("Failed to save recipe: ${e.message}")
        }
    }

    override fun addIngredient() {
        // Just call the view to add an ingredient field
        // The fragment will manage adding the actual ingredient to the list
        view?.addIngredientField()
    }

    override fun removeIngredient(position: Int) {
        // Just call the view to remove the ingredient
        // The fragment will manage removing the ingredient from the list
        view?.removeIngredientField(position)
    }
}
