package com.example.cocktaildb.screen.splash

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.databinding.ActivityFrontPageBinding
import com.example.cocktaildb.screen.auth.SignInActivity
import com.example.cocktaildb.utils.base.BaseActivity

class SplashActivity : BaseActivity<ActivityFrontPageBinding>(), SplashContract.View {

    private lateinit var presenter: SplashPresenter

    override fun inflateViewBinding(): ActivityFrontPageBinding {
        return ActivityFrontPageBinding.inflate(layoutInflater)
    }

    override fun initView() {
        supportActionBar?.hide()
        viewBinding.btnNavigateToCocktails.setOnClickListener {
            presenter.onStartButtonClicked()
        }
    }

    override fun initData() {
        val authRepository = AuthRepository(this)
        presenter = SplashPresenter(authRepository)
        presenter.setView(this)
        presenter.onStart()
    }

    override fun navigateToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun navigateToAuth() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        presenter.onStop()
        super.onStop()
    }
}
