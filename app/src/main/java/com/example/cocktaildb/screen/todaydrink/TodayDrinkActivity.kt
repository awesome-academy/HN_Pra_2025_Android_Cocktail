package com.example.cocktaildb.screen.todaydrink

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.ActivityTodayDrinkBinding
import com.example.cocktaildb.utils.base.BaseActivity
import com.example.cocktaildb.utils.TodayDrinkManager
import com.example.cocktaildb.utils.ImageLoader
import com.example.cocktaildb.screen.search.SearchActivity
import kotlinx.coroutines.launch
import android.view.View
import java.util.UUID

class TodayDrinkActivity : BaseActivity<ActivityTodayDrinkBinding>() {

    private lateinit var todayDrinkManager: TodayDrinkManager
    private var currentDrink: Cocktail? = null

    override fun inflateViewBinding(): ActivityTodayDrinkBinding {
        return ActivityTodayDrinkBinding.inflate(layoutInflater)
    }

    override fun initView() {
        todayDrinkManager = TodayDrinkManager(this)
        setupClickListeners()
        loadTodayDrink()
    }

    override fun initData() {
        // Initialize any data if needed
    }

    private fun setupClickListeners() {
        viewBinding.drinkCard.setOnClickListener {
            currentDrink?.let { drink ->
                navigateToCocktailDetail(drink)
            } ?: run {
                Toast.makeText(this, "No drink data available", Toast.LENGTH_SHORT).show()
            }
        }

        viewBinding.tvTitle.setOnLongClickListener {
            todayDrinkManager.forceRefresh()
            loadTodayDrink()
            Toast.makeText(this, "Refreshed today's drink!", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun loadTodayDrink() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val todayDrink = todayDrinkManager.getTodayDrink()
                todayDrink?.let { 
                    currentDrink = it
                    displayDrink(it)
                } ?: run {
                    val sampleDrink = createSampleDrink()
                    currentDrink = sampleDrink
                    displayDrink(sampleDrink)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@TodayDrinkActivity, 
                    "Failed to load today's drink", Toast.LENGTH_SHORT).show()
                val sampleDrink = createSampleDrink()
                currentDrink = sampleDrink
                displayDrink(sampleDrink)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            viewBinding.drinkCard.alpha = 0.5f
            viewBinding.drinkCard.isClickable = false
        } else {
            viewBinding.drinkCard.alpha = 1.0f
            viewBinding.drinkCard.isClickable = true
        }
    }

    private fun navigateToCocktailDetail(drink: Cocktail) {
        val intent = Intent(this, SearchActivity::class.java).apply {
            putExtra("show_detail", true)
            putExtra("from_today_drink", true)
            putExtra("cocktail_id", drink.idDrink)
            putExtra("cocktail_name", drink.strDrink)
            putExtra("cocktail_category", drink.strCategory)
            putExtra("cocktail_alcoholic", drink.strAlcoholic)
            putExtra("cocktail_glass", drink.strGlass)
            putExtra("cocktail_instructions", drink.strInstructions)
            putExtra("cocktail_image", drink.strDrinkThumb)
            putExtra("cocktail_ingredients", drink.ingredients.toTypedArray())
            putExtra("cocktail_measures", drink.measures.toTypedArray())
        }
        startActivity(intent)
    }

    private fun createSampleDrink(): Cocktail {
        val drinkNames = listOf(
            "Avocado Milkshake",
            "Chocolate Smoothie", 
            "Vanilla Latte",
            "Green Tea Frappe",
            "Strawberry Shake"
        )

        val categories = listOf("Drink", "Smoothie", "Coffee", "Tea", "Milkshake")

        return Cocktail(
            idDrink = "sample_${UUID.randomUUID()}",
            strDrink = drinkNames.random(),
            strCategory = categories.random(),
            strAlcoholic = "Non alcoholic",
            strGlass = "Glass",
            strInstructions = "Mix all ingredients and enjoy! Perfect for any time of the day.",
            strDrinkThumb = null,
            ingredients = listOf("Avocado", "Milk", "Sugar", "Ice"),
            measures = listOf("1", "200ml", "2 tsp", "As needed")
        )
    }

    private fun displayDrink(drink: Cocktail) {
        viewBinding.tvDrinkName.text = drink.strDrink
        viewBinding.tvDrinkCategory.text = drink.strCategory ?: "Drink"

        if (!drink.strDrinkThumb.isNullOrEmpty()) {
            ImageLoader.loadImage(
                drink.strDrinkThumb,
                viewBinding.ivDrinkImage,
                com.example.cocktaildb.R.mipmap.chocolate_milk
            )
        } else {
            viewBinding.ivDrinkImage.setImageResource(com.example.cocktaildb.R.mipmap.chocolate_milk)
        }

        val isHotDrink = drink.strCategory?.contains("Coffee", ignoreCase = true) == true ||
                         drink.strCategory?.contains("Tea", ignoreCase = true) == true ||
                         drink.strAlcoholic?.contains("Alcoholic", ignoreCase = true) == true ||
                         drink.strInstructions?.contains("hot", ignoreCase = true) == true

        if (isHotDrink) {
            viewBinding.cardHotBadge.visibility = View.VISIBLE
            viewBinding.tvHotBadge.text = "HOT"
        } else {
            viewBinding.cardHotBadge.visibility = View.GONE
        }
    }
}
