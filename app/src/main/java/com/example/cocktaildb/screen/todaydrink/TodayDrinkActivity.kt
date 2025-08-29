package com.example.cocktaildb.screen.todaydrink

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.cocktaildb.R
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

    companion object {
        private const val EXTRA_SHOW_DETAIL = "show_detail"
        private const val EXTRA_FROM_TODAY_DRINK = "from_today_drink"
        private const val EXTRA_COCKTAIL_ID = "cocktail_id"
        private const val EXTRA_COCKTAIL_NAME = "cocktail_name"
        private const val EXTRA_COCKTAIL_CATEGORY = "cocktail_category"
        private const val EXTRA_COCKTAIL_ALCOHOLIC = "cocktail_alcoholic"
        private const val EXTRA_COCKTAIL_GLASS = "cocktail_glass"
        private const val EXTRA_COCKTAIL_INSTRUCTIONS = "cocktail_instructions"
        private const val EXTRA_COCKTAIL_IMAGE = "cocktail_image"
        private const val EXTRA_COCKTAIL_INGREDIENTS = "cocktail_ingredients"
        private const val EXTRA_COCKTAIL_MEASURES = "cocktail_measures"
    }

    private lateinit var todayDrinkManager: TodayDrinkManager
    private var currentDrink: Cocktail? = null

    override fun inflateViewBinding(): ActivityTodayDrinkBinding {
        return ActivityTodayDrinkBinding.inflate(layoutInflater)
    }

    override fun initView() {
        todayDrinkManager = TodayDrinkManager.create(this)
        setupClickListeners()
        loadTodayDrink()
    }

    override fun initData() {
    }

    private fun setupClickListeners() {
        viewBinding.drinkCard.setOnClickListener {
            currentDrink?.let { drink ->
                navigateToCocktailDetail(drink)
            } ?: run {
                Toast.makeText(this, getString(R.string.msg_no_drink_data_available), Toast.LENGTH_SHORT).show()
            }
        }

        viewBinding.tvTitle.setOnLongClickListener {
            todayDrinkManager.forceRefresh()
            loadTodayDrink()
            Toast.makeText(this, getString(R.string.msg_refreshed_today_drink), Toast.LENGTH_SHORT).show()
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

                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@TodayDrinkActivity,
                    getString(R.string.msg_failed_load_today_drink), Toast.LENGTH_SHORT).show()

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
            putExtra(EXTRA_SHOW_DETAIL, true)
            putExtra(EXTRA_FROM_TODAY_DRINK, true)
            putExtra(EXTRA_COCKTAIL_ID, drink.idDrink)
            putExtra(EXTRA_COCKTAIL_NAME, drink.strDrink)
            putExtra(EXTRA_COCKTAIL_CATEGORY, drink.strCategory)
            putExtra(EXTRA_COCKTAIL_ALCOHOLIC, drink.strAlcoholic)
            putExtra(EXTRA_COCKTAIL_GLASS, drink.strGlass)
            putExtra(EXTRA_COCKTAIL_INSTRUCTIONS, drink.strInstructions)
            putExtra(EXTRA_COCKTAIL_IMAGE, drink.strDrinkThumb)
            putExtra(EXTRA_COCKTAIL_INGREDIENTS, drink.ingredients.toTypedArray())
            putExtra(EXTRA_COCKTAIL_MEASURES, drink.measures.toTypedArray())
        }
        startActivity(intent)
    }

    private fun displayDrink(drink: Cocktail) {
        viewBinding.tvDrinkName.text = drink.strDrink
        viewBinding.tvDrinkCategory.text = drink.strCategory ?: getString(R.string.default_drink_category)

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
            viewBinding.tvHotBadge.text = getString(R.string.hot_badge)
        } else {
            viewBinding.cardHotBadge.visibility = View.GONE
        }
    }
}
