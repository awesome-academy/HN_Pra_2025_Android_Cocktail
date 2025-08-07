package com.example.cocktaildb.screen.notifications

import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.base.BasePresenter

class NotificationsPresenter : NotificationsContract.Presenter {

    private var view: NotificationsContract.View? = null

    override fun setView(view: NotificationsContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // TODO: Initialize if needed
    }

    override fun onStop() {
        // TODO: Cleanup if needed
    }

    override fun loadNotifications() {
        (view as? BaseFragment<*>)?.showLoading()
        try {
            // TODO: Load notifications
            view?.showNotifications()
        } catch (e: Exception) {
            (view as? BaseFragment<*>)?.showError(e.message ?: "Unknown error")
        } finally {
            (view as? BaseFragment<*>)?.hideLoading()
        }
    }
}

