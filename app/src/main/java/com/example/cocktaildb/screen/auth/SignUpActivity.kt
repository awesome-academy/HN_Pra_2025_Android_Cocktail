package com.example.cocktaildb.screen.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository

class SignUpActivity : AppCompatActivity(), AuthContract.View {

    private lateinit var presenter: AuthPresenter
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnSignUp: Button
    private lateinit var btnGoogle: ImageView
    private lateinit var tvLoginLink: TextView

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
        setContentView(R.layout.activity_sign_up)

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
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGoogle = findViewById(R.id.btnGoogle)
        tvLoginLink = findViewById(R.id.tvLoginLink)
    }

    private fun setupClickListeners() {
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val termsAccepted = cbTerms.isChecked

            presenter.signUpWithEmail(name, email, password, confirmPassword, termsAccepted)
        }

        btnGoogle.setOnClickListener {
            presenter.loginWithGoogle()
        }

        tvLoginLink.setOnClickListener {
            // Quay về màn hình login
            finish()
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
