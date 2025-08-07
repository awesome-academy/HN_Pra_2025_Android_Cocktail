package com.example.cocktaildb.screen.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.databinding.FragmentHomeBinding
import com.example.cocktaildb.utils.base.BaseFragment

class HomeFragment : BaseFragment<FragmentHomeBinding>(), HomeContract.View {

    private lateinit var presenter: HomePresenter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater)
    }

    override fun initView() {
        // Initialize presenter
        val repository = CocktailRepository(CocktailLocalDataSource())
        presenter = HomePresenter(repository)
        presenter.setView(this)
    }

    override fun initData() {
        presenter.onStart()
        presenter.loadCocktails()
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }

    override fun showCocktails(cocktails: List<Cocktail>) {
        // TODO: Update UI with cocktails
        viewBinding.textHome.text = "Found ${cocktails.size} cocktails"
    }
}