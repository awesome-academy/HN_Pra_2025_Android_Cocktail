package com.example.cocktaildb.screen.dashboard

import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.base.BasePresenter

class DashboardPresenter : DashboardContract.Presenter {

    private var view: DashboardContract.View? = null

    override fun setView(view: DashboardContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // TODO: Initialize if needed
    }

    override fun onStop() {
        // TODO: Cleanup if needed
    }

    override fun loadDashboardData() {
        (view as? BaseFragment<*>)?.showLoading()
        try {
            // TODO: Load dashboard data
            view?.showDashboardData()
        } catch (e: Exception) {
            (view as? BaseFragment<*>)?.showError(e.message ?: "Unknown error")
        } finally {
            (view as? BaseFragment<*>)?.hideLoading()
        }
    }
}