package com.example.cocktaildb.screen.splash

import android.app.Activity
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository

class SplashPresenter(
    private val authRepository: AuthRepository
) : SplashContract.Presenter {

    private var view: SplashContract.View? = null
    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return if (view is Activity) {
            (view as Activity).getString(resId, *formatArgs)
        } else {
            (view as? Activity)?.getString(R.string.msg_unavailable) ?: "Message unavailable"
        }
    }
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
            view?.showMessage(getString(R.string.Welcome))
        } else {
            view?.showMessage(getString(R.string.Welcome_back))
        }
    }

    override fun onStartButtonClicked() {
        view?.navigateToAuth()
    }
}
