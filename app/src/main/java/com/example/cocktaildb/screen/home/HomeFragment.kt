package com.example.cocktaildb.screen.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.databinding.FragmentHomeBinding
import com.example.cocktaildb.utils.base.BaseFragment
import com.bumptech.glide.Glide
import kotlin.random.Random

class HomeFragment : BaseFragment<FragmentHomeBinding>(), HomeContract.View {

    private lateinit var presenter: HomePresenter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater)
    }

    override fun initView() {
        // Initialize presenter with remote data source
        val repository = CocktailRepository(CocktailRemoteDataSource())
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
        if (cocktails.isEmpty()) {
            Toast.makeText(context, "No cocktails found", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Skip first 3 cocktails and show from 4th onwards, limit to 4 cocktails
        val filteredCocktails = if (cocktails.size > 3) {
            val startIndex = 3
            val endIndex = minOf(startIndex + 4, cocktails.size)
            cocktails.subList(startIndex, endIndex)
        } else {
            // If less than 4 cocktails, show all
            cocktails
        }
        
        if (filteredCocktails.isEmpty()) {
            Toast.makeText(context, "No cocktails available after filtering", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Update UI with filtered cocktails
        updateCocktailGrid(filteredCocktails)
        
        // Show success message
        Toast.makeText(context, "Loaded ${filteredCocktails.size} cocktails", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateCocktailGrid(cocktails: List<Cocktail>) {
        // Update first cocktail
        if (cocktails.isNotEmpty()) {
            updateCocktailCard(
                viewBinding.tvCocktailName1,
                viewBinding.tvCocktailCategory1,
                viewBinding.tvRating1,
                viewBinding.ivCocktail1,
                cocktails[0]
            )
        }
        
        // Update second cocktail
        if (cocktails.size > 1) {
            updateCocktailCard(
                viewBinding.tvCocktailName2,
                viewBinding.tvCocktailCategory2,
                viewBinding.tvRating2,
                viewBinding.ivCocktail2,
                cocktails[1]
            )
        }
        
        // Update third cocktail
        if (cocktails.size > 2) {
            updateCocktailCard(
                viewBinding.tvCocktailName3,
                viewBinding.tvCocktailCategory3,
                viewBinding.tvRating3,
                viewBinding.ivCocktail3,
                cocktails[2]
            )
        }
        
        // Update fourth cocktail
        if (cocktails.size > 3) {
            updateCocktailCard(
                viewBinding.tvCocktailName4,
                viewBinding.tvCocktailCategory4,
                viewBinding.tvRating4,
                viewBinding.ivCocktail4,
                cocktails[3]
            )
        }
    }
    
    private fun updateCocktailCard(
        nameTextView: android.widget.TextView,
        categoryTextView: android.widget.TextView,
        ratingTextView: android.widget.TextView,
        imageView: android.widget.ImageView,
        cocktail: Cocktail
    ) {
        nameTextView.text = cocktail.strDrink
        categoryTextView.text = cocktail.strCategory ?: "Cocktail"
        
        // Generate random rating between 4.0 and 5.0
        val rating = (Random.nextDouble(4.0, 5.0) * 10).toInt() / 10.0
        ratingTextView.text = rating.toString()
        
        // Load cocktail image using Glide
        if (!cocktail.strDrinkThumb.isNullOrEmpty()) {
            Glide.with(this)
                .load(cocktail.strDrinkThumb)
                .placeholder(com.example.cocktaildb.R.drawable.imgstart)
                .error(com.example.cocktaildb.R.drawable.imgstart)
                .into(imageView)
        } else {
            // Use placeholder if no image URL
            imageView.setImageResource(com.example.cocktaildb.R.drawable.imgstart)
        }
        
        // Set click listener for the card
        (imageView.parent as? android.view.View)?.setOnClickListener {
            Toast.makeText(context, "Selected: ${cocktail.strDrink}", Toast.LENGTH_SHORT).show()
        }
    }
}

