package com.example.cocktaildb.screen.splash

import com.example.cocktaildb.data.repository.AuthRepository

class SplashPresenter(
    private val authRepository: AuthRepository
) : SplashContract.Presenter {

    private var view: SplashContract.View? = null

    override fun setView(view: SplashContract.View?) {
        this.view = view
    }

    override fun onStart() {
        checkUserLoginStatus()
    }

    override fun onStop() {
        // TODO: Cleanup if needed
    }

    override fun checkUserLoginStatus() {
        if (authRepository.isUserLoggedIn()) {
            view?.showMessage("Welcome back!")
        } else {
            view?.showMessage("Welcome! Please login to continue.")
        }
    }

    override fun onStartButtonClicked() {
        view?.navigateToAuth()
    }
}
