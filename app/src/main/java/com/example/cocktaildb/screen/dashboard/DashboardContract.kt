package com.example.cocktaildb.screen.dashboard

import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.utils.base.BasePresenter

interface DashboardContract {
    interface View {
        fun showDashboardData()
        fun showTodayDrink(cocktail: Cocktail)
        fun showError(message: String)
        fun showMessage(message: String)
        fun navigateToCocktailDetail(cocktail: Cocktail)
    }

    interface Presenter : BasePresenter<View> {
        fun loadDashboardData()
        fun loadTodayDrink()
        fun refreshTodayDrink()
        fun onDrinkCardClick()
        fun navigateToCocktailDetail(cocktail: Cocktail)
        override fun setView(view: View?)
        override fun onStart()
        override fun onStop()
    }
}

