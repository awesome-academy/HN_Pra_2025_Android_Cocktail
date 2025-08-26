package com.example.cocktaildb.screen.notifications

import com.example.cocktaildb.utils.base.BasePresenter

interface NotificationsContract {
    interface View {
        fun showNotifications()
        fun showNotificationSent()
        fun showNotificationScheduled()
        fun showNotificationCancelled()
        fun showLoading()
        fun hideLoading()
        fun showError(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadNotifications()
        fun testNotification()
        fun scheduleDailyNotification()
        fun cancelDailyNotification()
    }
}

