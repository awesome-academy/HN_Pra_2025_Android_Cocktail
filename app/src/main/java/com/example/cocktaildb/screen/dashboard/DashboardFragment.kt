package com.example.cocktaildb.screen.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.FragmentDashboardBinding
import com.example.cocktaildb.utils.TodayDrinkManager
import com.example.cocktaildb.utils.ImageLoader
import com.example.cocktaildb.screen.search.SearchActivity
import com.example.cocktaildb.utils.base.BaseFragment
import kotlinx.coroutines.launch

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
    private lateinit var todayDrinkManager: TodayDrinkManager
    private var currentDrink: Cocktail? = null

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentDashboardBinding {
        return FragmentDashboardBinding.inflate(inflater)
    }

    override fun initView() {
        // Initialize presenter
        presenter = DashboardPresenter()
        presenter.setView(this)

        todayDrinkManager = TodayDrinkManager(requireContext())
        setupClickListeners()
    }

    private fun setupClickListeners() {
        try {
            viewBinding.drinkCard?.setOnClickListener {
                currentDrink?.let { drink ->
                    navigateToCocktailDetail(drink)
                } ?: run {
                    Toast.makeText(requireContext(), getString(R.string.msg_no_drink_data_available), Toast.LENGTH_SHORT).show()
                }
            }

            viewBinding.tvTitle?.setOnLongClickListener {
                todayDrinkManager.forceRefresh()
                loadTodayDrink()
                Toast.makeText(requireContext(), getString(R.string.msg_refreshed_today_drink), Toast.LENGTH_SHORT).show()
                true
            }
        } catch (e: Exception) {
            showDashboardData()
        }
    }

    override fun initData() {
        presenter.onStart()
        presenter.loadDashboardData()
        loadTodayDrink()
    }

    private fun loadTodayDrink() {
        lifecycleScope.launch {
            try {
                val todayDrink = todayDrinkManager.getTodayDrink()
                todayDrink?.let { 
                    currentDrink = it
                    displayDrink(it)
                } ?: run {
                    Toast.makeText(requireContext(), getString(R.string.msg_no_today_drink_available), Toast.LENGTH_SHORT).show()
                    showDashboardData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), getString(R.string.msg_failed_load_today_drink), Toast.LENGTH_SHORT).show()
                showDashboardData()
            }
        }
    }

    private fun navigateToCocktailDetail(drink: Cocktail) {
        val intent = Intent(requireContext(), SearchActivity::class.java).apply {
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
        try {
            viewBinding.tvDrinkName?.text = drink.strDrink
            viewBinding.tvDrinkCategory?.text = drink.strCategory ?: getString(R.string.default_drink_category)

            viewBinding.ivDrinkImage?.let { imageView ->
                if (!drink.strDrinkThumb.isNullOrEmpty()) {
                    ImageLoader.loadImage(
                        drink.strDrinkThumb,
                        imageView,
                        R.mipmap.chocolate_milk
                    )
                } else {
                    imageView.setImageResource(R.mipmap.chocolate_milk)
                }
            }

            val isHotDrink = drink.strCategory?.contains("Coffee", ignoreCase = true) == true ||
                             drink.strCategory?.contains("Tea", ignoreCase = true) == true ||
                             drink.strAlcoholic?.contains("Alcoholic", ignoreCase = true) == true ||
                             drink.strInstructions?.contains("hot", ignoreCase = true) == true

            viewBinding.cardHotBadge?.visibility = if (isHotDrink) View.VISIBLE else View.GONE
            viewBinding.tvHotBadge?.text = getString(R.string.hot_badge)
        } catch (e: Exception) {
            showDashboardData()
        }
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }

    override fun showDashboardData() {
        viewBinding.textDashboard?.text = currentDrink?.strDrink ?: getString(R.string.msg_today_special_drink)
    }
}

