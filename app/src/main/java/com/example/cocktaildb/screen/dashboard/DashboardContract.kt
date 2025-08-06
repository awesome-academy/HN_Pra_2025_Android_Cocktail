package com.example.cocktaildb.screen.dashboard

import com.example.cocktaildb.utils.base.BasePresenter

interface DashboardContract {
    interface View {
        fun showDashboardData()
        fun showError(message: String)
        fun showLoading()
        fun hideLoading()
    }
    
    interface Presenter : BasePresenter<View> {
        fun loadDashboardData()
    }
} 