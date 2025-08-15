package com.example.cocktaildb.screen.myrecipe

import android.os.Handler
import android.os.Looper
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import java.util.concurrent.Executors


class MyRecipePresenter(
    private val cocktailRepository: CocktailRepository
) : MyRecipeContract.Presenter {

    private var view: MyRecipeContract.View? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun setView(view: MyRecipeContract.View?) {
        this.view = view
        if (view != null) {
            loadUserRecipes()
        }
    }

    override fun onStart() {
        // Refresh data if needed
    }

    override fun onStop() {
        // Clean up resources
        view = null
    }

    override fun loadUserRecipes() {
        view?.displayLoading(true)

        // Using a simple background thread executor instead of AsyncTask
        executor.execute {
            // In a real app, this would fetch from repository
            // For now, we'll use mock data
            val recipes = createMockUserRecipes()

            // Post results back to main thread
            mainHandler.post {
                view?.displayLoading(false)
                view?.showUserRecipes(recipes)
            }
        }
    }


    private fun createMockUserRecipes(): List<Cocktail> {
        return listOf(
            Cocktail(
                idDrink = "101",
                strDrink = "Berry Blast",
                strCategory = "My special berry cocktail",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/xwqvur1468876473.jpg"
            ),
            Cocktail(
                idDrink = "102",
                strDrink = "Mango Tango",
                strCategory = "Sweet mango-based cocktail with a twist",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/vrwquq1478252802.jpg"
            ),
            Cocktail(
                idDrink = "103",
                strDrink = "Blue Lagoon",
                strCategory = "My take on the blue lagoon - with extra citrus",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/5wm4zo1582579154.jpg"
            ),
            Cocktail(
                idDrink = "104",
                strDrink = "Vanilla Dream",
                strCategory = "Creamy vanilla cocktail perfect for dessert",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/yrhutv1503563730.jpg"
            ),
            Cocktail(
                idDrink = "105",
                strDrink = "Ginger Fizz",
                strCategory = "Spicy ginger cocktail with soda",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/nzlyc81605905755.jpg"
            ),
            Cocktail(
                idDrink = "106",
                strDrink = "Cucumber Cooler",
                strCategory = "Refreshing cucumber-based summer drink",
                strDrinkThumb = "https://www.thecocktaildb.com/images/media/drink/hbkfsh1589574990.jpg"
            )
        )
    }
}
