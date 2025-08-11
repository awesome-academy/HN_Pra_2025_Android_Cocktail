package com.example.cocktaildb.screen.splash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.screen.auth.SignInActivity

class SplashFragment : Fragment(), SplashContract.View {

    private lateinit var presenter: SplashPresenter
    private lateinit var btnStart: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_front_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPresenter()
        initViews(view)
    }

    private fun initPresenter() {
        val authRepository = AuthRepository()
        presenter = SplashPresenter(authRepository)
        presenter.setView(this)
        presenter.onStart()
    }

    private fun initViews(view: View) {
        btnStart = view.findViewById(R.id.btnNavigateToCocktails)
        btnStart.setOnClickListener {
            presenter.onStartButtonClicked()
        }
    }

    override fun navigateToHome() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun navigateToAuth() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }
}
