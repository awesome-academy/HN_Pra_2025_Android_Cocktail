package com.example.cocktaildb.screen.splash

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.screen.auth.SignInActivity

class SplashActivity : AppCompatActivity(), SplashContract.View {

    private lateinit var presenter: SplashPresenter
    private lateinit var btnStart: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_front_page)
        
        initPresenter()
        initViews()
    }

    private fun initPresenter() {
        val authRepository = AuthRepository()
        presenter = SplashPresenter(authRepository)
        presenter.setView(this)
        presenter.onStart()
    }

    private fun initViews() {
        btnStart = findViewById(R.id.btnNavigateToCocktails)
        btnStart.setOnClickListener {
            presenter.onStartButtonClicked()
        }
    }

    override fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun navigateToAuth() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        presenter.onStop()
        super.onDestroy()
    }
}
