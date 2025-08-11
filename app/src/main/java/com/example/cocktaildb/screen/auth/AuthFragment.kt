package com.example.cocktaildb.screen.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository

class AuthFragment : Fragment(), AuthContract.View {

    private lateinit var presenter: AuthPresenter
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnGoogle: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPresenter()
        initViews(view)
        setupClickListeners()
        presenter.onStart()
    }

    private fun initPresenter() {
        val authRepository = AuthRepository()
        presenter = AuthPresenter(authRepository)
        presenter.setView(this)
    }

    private fun initViews(view: View) {
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnSignIn = view.findViewById(R.id.btnSignIn)
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword)
        btnGoogle = view.findViewById(R.id.btnGoogle)
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
    }

    override fun onLoginSuccess() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
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
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun launchGoogleSignIn(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }
}
