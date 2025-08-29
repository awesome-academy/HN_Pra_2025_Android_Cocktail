package com.example.cocktaildb.screen.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.FragmentDashboardBinding
import com.example.cocktaildb.utils.TodayDrinkManager
import com.example.cocktaildb.utils.ImageLoader
import com.example.cocktaildb.screen.search.SearchActivity
import com.example.cocktaildb.utils.base.BaseFragment

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(), DashboardContract.View {

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

    private lateinit var presenter: DashboardPresenter
    private var currentDrink: Cocktail? = null

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentDashboardBinding {
        return FragmentDashboardBinding.inflate(inflater)
    }

    override fun initView() {
        // Initialize presenter with dependency injection
        val todayDrinkManager = TodayDrinkManager.create(requireContext())
        presenter = DashboardPresenter(todayDrinkManager) // Default dispatchers được sử dụng
        presenter.setView(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        try {
            viewBinding.drinkCard?.setOnClickListener {
                currentDrink?.let { drink ->
                    presenter.navigateToCocktailDetail(drink)
                } ?: run {
                    showMessage(getString(R.string.msg_no_drink_data_available))
                }
            }

            viewBinding.tvTitle?.setOnLongClickListener {
                presenter.refreshTodayDrink()
                true
            }
        } catch (e: Exception) {
            showDashboardData()
        }
    }

    override fun initData() {
        presenter.onStart()
        presenter.loadDashboardData()
        presenter.loadTodayDrink()
    }

    override fun showTodayDrink(cocktail: Cocktail) {
        displayTodayDrink(cocktail)
    }

    override fun navigateToCocktailDetail(cocktail: Cocktail) {
        navigateToDetail(cocktail)
    }

    private fun displayTodayDrink(cocktail: Cocktail) {
        currentDrink = cocktail
        try {
            viewBinding.tvDrinkName?.text = cocktail.strDrink
            viewBinding.tvDrinkCategory?.text = cocktail.strCategory ?: getString(R.string.default_drink_category)

            viewBinding.ivDrinkImage?.let { imageView ->
                if (!cocktail.strDrinkThumb.isNullOrEmpty()) {
                    ImageLoader.loadImage(
                        cocktail.strDrinkThumb,
                        imageView,
                        R.mipmap.chocolate_milk
                    )
                } else {
                    imageView.setImageResource(R.mipmap.chocolate_milk)
                }
            }

            val isHotDrink = cocktail.strCategory?.contains("Coffee", ignoreCase = true) == true ||
                    cocktail.strCategory?.contains("Tea", ignoreCase = true) == true ||
                    cocktail.strAlcoholic?.contains("Alcoholic", ignoreCase = true) == true ||
                    cocktail.strInstructions?.contains("hot", ignoreCase = true) == true

            viewBinding.cardHotBadge?.visibility = if (isHotDrink) View.VISIBLE else View.GONE
            viewBinding.tvHotBadge?.text = getString(R.string.hot_badge)
        } catch (e: Exception) {
            showDashboardData()
        }
    }

    private fun navigateToDetail(cocktail: Cocktail) {
        val intent = Intent(requireContext(), SearchActivity::class.java).apply {
            putExtra(EXTRA_SHOW_DETAIL, true)
            putExtra(EXTRA_FROM_TODAY_DRINK, true)
            putExtra(EXTRA_COCKTAIL_ID, cocktail.idDrink)
            putExtra(EXTRA_COCKTAIL_NAME, cocktail.strDrink)
            putExtra(EXTRA_COCKTAIL_CATEGORY, cocktail.strCategory)
            putExtra(EXTRA_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic)
            putExtra(EXTRA_COCKTAIL_GLASS, cocktail.strGlass)
            putExtra(EXTRA_COCKTAIL_INSTRUCTIONS, cocktail.strInstructions)
            putExtra(EXTRA_COCKTAIL_IMAGE, cocktail.strDrinkThumb)
            putExtra(EXTRA_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
            putExtra(EXTRA_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
        }
        startActivity(intent)
    }

    override fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }

    override fun showDashboardData() {
        viewBinding.textDashboard?.text = currentDrink?.strDrink ?: getString(R.string.msg_today_special_drink)
    }
}

