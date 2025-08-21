package com.example.cocktaildb.screen.auth

import android.content.Intent
import com.example.cocktaildb.utils.base.BasePresenter

interface AuthContract {
    interface View {
        fun onLoginSuccess()
        fun onLoginFailure(error: String)
        fun showLoading()
        fun hideLoading()
        fun showMessage(message: String)
        fun launchGoogleSignIn(intent: Intent)
    }

    interface Presenter : BasePresenter<View> {
        fun loginWithEmail(email: String, password: String)
        fun signUpWithEmail(name: String, email: String, password: String, confirmPassword: String, termsAccepted: Boolean)
        fun loginWithGoogle()
        fun forgotPassword(email: String)
        fun validateLoginInput(email: String, password: String): Boolean
        fun validateSignUpInput(name: String, email: String, password: String, confirmPassword: String, termsAccepted: Boolean): Boolean
    }
}
