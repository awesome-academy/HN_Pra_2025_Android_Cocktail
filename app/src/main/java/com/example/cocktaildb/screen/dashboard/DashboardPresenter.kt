package com.example.cocktaildb.screen.dashboard

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.TodayDrinkManager
import com.example.cocktaildb.utils.base.BaseFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardPresenter(
    private val todayDrinkManager: TodayDrinkManager
) : DashboardContract.Presenter {

    private var view: DashboardContract.View? = null

    override fun setView(view: DashboardContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // Initialize if needed
    }

    override fun onStop() {
        // Cleanup if needed
    }

    override fun loadDashboardData() {
        view?.showDashboardData()
    }

    override fun loadTodayDrink() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todayDrink = todayDrinkManager.getTodayDrink()
                withContext(Dispatchers.Main) {
                    todayDrink?.let {
                        view?.showTodayDrink(it)
                    } ?: run {
                        view?.showMessage("No today drink available")
                        view?.showDashboardData()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view?.showMessage("Failed to load today drink")
                    view?.showDashboardData()
                }
            }
        }
    }

    override fun refreshTodayDrink() {
        todayDrinkManager.forceRefresh()
        loadTodayDrink()
        view?.showMessage("Refreshed today drink")
    }

    override fun onDrinkCardClick() {
        // Implementation for drink card click if needed
    }

    override fun navigateToCocktailDetail(drink: Cocktail) {
        view?.navigateToCocktailDetail(drink)
    }
}

