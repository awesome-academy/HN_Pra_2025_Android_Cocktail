package com.example.cocktaildb.screen.auth

import android.app.Activity
import android.content.Intent
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.model.LoginMethod
import com.example.cocktaildb.utils.GoogleAuth

class AuthPresenter(
    private val authRepository: AuthRepository
) : AuthContract.Presenter {

    private var view: AuthContract.View? = null
    private var googleAuthHelper: GoogleAuth? = null

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return if (view is Activity) {
            (view as Activity).getString(resId, *formatArgs)
        } else {
            (view as? Activity)?.getString(R.string.msg_unavailable) ?: "Message unavailable"
        }
    }

    override fun setView(view: AuthContract.View?) {
        this.view = view
        if (view is Activity) {
            googleAuthHelper = GoogleAuth(view)
        }
    }

    override fun onStart() {}
    override fun onStop() {}

    override fun loginWithEmail(email: String, password: String) {
        if (!validateLoginInput(email, password)) return

        view?.showLoading()
        authRepository.loginWithEmail(email, password) { firebaseUser, error ->
            view?.hideLoading()
            if (firebaseUser != null) {
                view?.onLoginSuccess()
            } else {
                view?.onLoginFailure(error ?: getString(R.string.msg_email_or_password_incorrect))
            }
        }
    }

    override fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean
    ) {
        if (!validateSignUpInput(name, email, password, confirmPassword, termsAccepted)) return

        view?.showLoading()
        authRepository.signUpWithEmail(name, email, password) { firebaseUser, error ->
            view?.hideLoading()
            if (firebaseUser != null) {
                view?.onLoginSuccess()
            } else {
                view?.onLoginFailure(error ?: getString(R.string.msg_signup_failed))
            }
        }
    }

    override fun loginWithGoogle() {
        googleAuthHelper?.let { helper ->
            view?.showLoading()
            val signInIntent = helper.getSignInIntent()
            if (view is SignInActivity) {
                (view as SignInActivity).launchGoogleSignIn(signInIntent)
            } else if (view is SignUpActivity) {
                (view as SignUpActivity).launchGoogleSignIn(signInIntent)
            } else {
                view?.showMessage(getString(R.string.msg_google_login_unavailable))
                view?.hideLoading()
            }
        } ?: run {
            view?.showMessage(getString(R.string.msg_google_login_not_initialized))
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        googleAuthHelper?.handleSignInResult(
            data,
            onSuccess = { account ->
                val firebaseUser = authRepository.getCurrentUser()
                if (firebaseUser != null) {
                    authRepository.saveUserToFirestore(
                        firebaseUser = firebaseUser,
                        name = account.displayName ?: "",
                        loginMethod = LoginMethod.GOOGLE,
                        profileImage = account.photoUrl?.toString()
                    ) { success, error ->
                        view?.hideLoading()
                        if (success) {
                            view?.onLoginSuccess()
                        } else {
                            view?.onLoginFailure(
                                getString(R.string.msg_failed_to_save_user, error ?: "Unknown error")
                            )
                        }
                    }
                } else {
                    view?.hideLoading()
                    view?.onLoginFailure(getString(R.string.msg_firebase_user_not_exist))
                }
            },
            onFailure = { error ->
                view?.hideLoading()
                view?.onLoginFailure(getString(R.string.msg_google_signin_failed, error))
            }
        )
    }

    override fun forgotPassword(email: String) {
        if (email.isEmpty()) {
            view?.showMessage(getString(R.string.msg_enter_email))
            return
        }

        view?.showLoading()
        authRepository.sendPasswordResetEmail(email) { success, error ->
            view?.hideLoading()
            if (success) {
                view?.showMessage(getString(R.string.msg_check_inbox_reset_password))
            } else {
                view?.showMessage(
                    getString(R.string.msg_unable_to_send_email, error ?: "An error occurred")
                )
            }
        }
    }

    override fun validateLoginInput(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                view?.showMessage(getString(R.string.msg_enter_email))
                false
            }
            password.isEmpty() -> {
                view?.showMessage(getString(R.string.msg_enter_password))
                false
            }
            else -> true
        }
    }

    override fun validateSignUpInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean
    ): Boolean {
        return when {
            name.isEmpty() -> {
                view?.showMessage(getString(R.string.msg_enter_name))
                false
            }
            email.isEmpty() -> {
                view?.showMessage(getString(R.string.msg_enter_email))
                false
            }
            password.isEmpty() -> {
                view?.showMessage(getString(R.string.msg_enter_password))
                false
            }
            confirmPassword.isEmpty() -> {
                view?.showMessage(getString(R.string.msg_confirm_password))
                false
            }
            password != confirmPassword -> {
                view?.showMessage(getString(R.string.msg_password_mismatch))
                false
            }
            !termsAccepted -> {
                view?.showMessage(getString(R.string.msg_accept_terms))
                false
            }
            else -> true
        }
    }
}
