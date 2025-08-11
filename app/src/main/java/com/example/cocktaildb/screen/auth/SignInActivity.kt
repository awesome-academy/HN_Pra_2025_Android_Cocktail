package com.example.cocktaildb.screen.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository

class SignInActivity : AppCompatActivity(), AuthContract.View {

    private lateinit var presenter: AuthPresenter
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnGoogle: ImageView
    private lateinit var tvSignUp: TextView

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                presenter.handleGoogleSignInResult(result.data)
            } else {
                hideLoading()
                showMessage("Google Sign-In bị hủy")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)
        
        initPresenter()
        initViews()
        setupClickListeners()
        presenter.onStart()
    }

    private fun initPresenter() {
        val authRepository = AuthRepository()
        presenter = AuthPresenter(authRepository)
        presenter.setView(this)
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnGoogle = findViewById(R.id.btnGoogle)
        tvSignUp = findViewById(R.id.tvSignUp)
    }

    private fun setupClickListeners() {
        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            presenter.loginWithEmail(email, password)
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            presenter.forgotPassword(email)
        }

        btnGoogle.setOnClickListener {
            presenter.loginWithGoogle()
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onLoginSuccess() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onLoginFailure(error: String) {
        showMessage(error)
    }

    override fun showLoading() {
        // TODO: Show loading dialog or progress bar
    }

    override fun hideLoading() {
        // TODO: Hide loading dialog or progress bar
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun launchGoogleSignIn(intent: Intent) {
        googleSignInLauncher.launch(intent)
    }

    override fun onDestroy() {
        presenter.onStop()
        super.onDestroy()
    }
}
