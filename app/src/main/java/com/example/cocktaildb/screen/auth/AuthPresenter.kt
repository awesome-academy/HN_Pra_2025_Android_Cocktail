package com.example.cocktaildb.screen.auth

import android.app.Activity
import android.content.Intent
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.model.LoginMethod
import com.example.cocktaildb.utils.GoogleAuth

class AuthPresenter(
    private val authRepository: AuthRepository
) : AuthContract.Presenter {

    private var view: AuthContract.View? = null
    private var googleAuthHelper: GoogleAuth? = null

    override fun setView(view: AuthContract.View?) {
        this.view = view
        if (view is Activity) {
            googleAuthHelper = GoogleAuth(view as Activity)
        }
    }

    override fun onStart() {
        // TODO: Initialize if needed
    }

    override fun onStop() {
        // TODO: Cleanup if needed
    }

    override fun loginWithEmail(email: String, password: String) {
        if (!validateLoginInput(email, password)) return

        view?.showLoading()
        authRepository.loginWithEmail(email, password) { firebaseUser, error ->
            view?.hideLoading()
            if (firebaseUser != null) {
                view?.onLoginSuccess()
            } else {
                view?.onLoginFailure(error ?: "Email hoặc password không đúng")
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
                view?.onLoginFailure(error ?: "Đăng ký thất bại")
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
                view?.showMessage("Google login không khả dụng")
                view?.hideLoading()
            }
        } ?: run {
            view?.showMessage("Google login chưa được khởi tạo")
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
                            view?.onLoginFailure("Lưu thông tin user thất bại: ${error ?: "Unknown error"}")
                        }
                    }
                } else {
                    view?.hideLoading()
                    view?.onLoginFailure("Firebase user không tồn tại")
                }
            },
            onFailure = { error ->
                view?.hideLoading()
                view?.onLoginFailure("Google Sign-In thất bại: $error")
            }
        )
    }

    override fun forgotPassword(email: String) {
        if (email.isEmpty()) {
            view?.showMessage("Vui lòng nhập email")
            return
        }

        view?.showLoading()
        authRepository.sendPasswordResetEmail(email) { success, error ->
            view?.hideLoading()
            if (success) {
                view?.showMessage("Vui lòng kiểm tra hòm thư để thay đổi mật khẩu")
            } else {
                view?.showMessage("Không thể gửi email: ${error ?: "Đã xảy ra lỗi"}")
            }
        }
    }

    override fun validateLoginInput(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                view?.showMessage("Vui lòng nhập email")
                false
            }
            password.isEmpty() -> {
                view?.showMessage("Vui lòng nhập password")
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
                view?.showMessage("Vui lòng nhập tên")
                false
            }
            email.isEmpty() -> {
                view?.showMessage("Vui lòng nhập email")
                false
            }
            password.isEmpty() -> {
                view?.showMessage("Vui lòng nhập password")
                false
            }
            confirmPassword.isEmpty() -> {
                view?.showMessage("Vui lòng nhập xác nhận password")
                false
            }
            password != confirmPassword -> {
                view?.showMessage("Password không khớp")
                false
            }
            !termsAccepted -> {
                view?.showMessage("Vui lòng đồng ý điều khoản")
                false
            }
            else -> true
        }
    }
}
