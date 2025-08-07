package com.example.cocktaildb.screen.notifications

import com.example.cocktaildb.utils.base.BasePresenter

interface NotificationsContract {
    interface View {
        fun showNotifications()
    }

    interface Presenter : BasePresenter<View> {
        fun loadNotifications()
    }
}

