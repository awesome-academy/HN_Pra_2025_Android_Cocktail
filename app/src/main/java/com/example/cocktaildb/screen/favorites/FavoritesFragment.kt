package com.example.cocktaildb.screen.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.FragmentFavoritesBinding
import com.example.cocktaildb.databinding.ItemCocktailBinding
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.ImageLoader

class FavoritesFragment : BaseFragment<FragmentFavoritesBinding>(), FavoritesContract.View {

    private val presenter = FavoritesPresenter()
    private val favoritesAdapter = FavoritesAdapter()

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentFavoritesBinding {
        return FragmentFavoritesBinding.inflate(inflater)
    }

    override fun initView() {
        setupRecyclerView()
    }

    override fun initData() {
        presenter.setView(this)
        presenter.onStart()
    }

    override fun onDestroyView() {
        presenter.setView(null)
        presenter.onStop()
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        viewBinding.favoritesRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = favoritesAdapter
        }
    }

    // Implement FavoritesContract.View methods with names that don't conflict with BaseFragment
    override fun displayLoading(show: Boolean) {
        if (show) {
            super.showLoading()
            viewBinding.favoritesRecyclerView.visibility = View.GONE
            viewBinding.emptyStateView.visibility = View.GONE
        } else {
            super.hideLoading()
        }
    }

    override fun displayFavorites(favorites: List<Cocktail>) {
        viewBinding.favoritesRecyclerView.visibility = View.VISIBLE
        viewBinding.emptyStateView.visibility = View.GONE
        favoritesAdapter.submitList(favorites)
    }

    override fun displayEmptyState() {
        viewBinding.favoritesRecyclerView.visibility = View.GONE
        viewBinding.emptyStateView.visibility = View.VISIBLE
    }

    override fun displayError(message: String) {
        super.showError(message)
    }

    inner class FavoritesAdapter : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {
        private var cocktails: List<Cocktail> = emptyList()

        fun submitList(cocktails: List<Cocktail>) {
            this.cocktails = cocktails
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCocktailBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(cocktails[position])
        }

        override fun getItemCount(): Int = cocktails.size

        inner class ViewHolder(private val binding: ItemCocktailBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(cocktail: Cocktail) {
                // Use correct IDs from item_cocktail.xml layout
                binding.tvCocktailName.text = cocktail.strDrink
                binding.tvCocktailCategory.text = cocktail.strCategory ?: "Cocktail"

                // Load image using the ImageLoader class
                ImageLoader.loadImage(cocktail.strDrinkThumb, binding.ivCocktail, R.drawable.imgstart)

                // Set click listener to navigate to details
                binding.root.setOnClickListener {
                    navigateToDetailScreen(cocktail)
                }
            }

            private fun navigateToDetailScreen(cocktail: Cocktail) {
                // Extract ingredients and measures from the cocktail object
                // Using a safe approach to handle potential null values
                val ingredients = cocktail.ingredients.toTypedArray()
                val measures = cocktail.measures.toTypedArray()

                findNavController().navigate(
                    R.id.navigation_cocktail_detail,
                    Bundle().apply {
                        putString("cocktail_id", cocktail.idDrink)
                        putString("cocktail_name", cocktail.strDrink)
                        putString("cocktail_category", cocktail.strCategory)
                        putString("cocktail_alcoholic", cocktail.strAlcoholic)
                        putString("cocktail_glass", cocktail.strGlass)
                        putString("cocktail_instructions", cocktail.strInstructions)
                        putString("cocktail_image", cocktail.strDrinkThumb)
                        putStringArray("cocktail_ingredients", ingredients)
                        putStringArray("cocktail_measures", measures)
                    }
                )
            }
        }
    }
}
