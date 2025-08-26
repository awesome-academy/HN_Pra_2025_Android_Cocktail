package com.example.cocktaildb.screen.notifications

import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.base.BasePresenter

class NotificationsPresenter : NotificationsContract.Presenter {

    private var view: NotificationsContract.View? = null

    override fun setView(view: NotificationsContract.View?) {
        this.view = view
    }

    override fun onStart() {
        // Initialize if needed
    }

    override fun onStop() {
        // Cleanup if needed
    }

    override fun loadNotifications() {
        view?.showLoading()
        try {
            // Load notifications
            view?.showNotifications()
        } catch (e: Exception) {
            view?.showError(e.message ?: "Unknown error")
        } finally {
            view?.hideLoading()
        }
    }

    override fun testNotification() {
        view?.showNotificationSent()
    }

    override fun scheduleDailyNotification() {
        view?.showNotificationScheduled()
    }

    override fun cancelDailyNotification() {
        view?.showNotificationCancelled()
    }
}

