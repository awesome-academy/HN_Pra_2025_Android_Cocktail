package com.example.cocktaildb.screen.splash

import com.example.cocktaildb.utils.base.BasePresenter

interface SplashContract {
    interface View {
        fun navigateToHome()
        fun navigateToAuth()
        fun showMessage(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun checkUserLoginStatus()
        fun onStartButtonClicked()
    }
}
