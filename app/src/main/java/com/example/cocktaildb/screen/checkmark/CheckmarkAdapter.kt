package com.example.cocktaildb.screen.checkmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.ItemCocktailBinding
import com.example.cocktaildb.utils.ImageLoader

class CheckmarkAdapter(
    private val presenter: CheckmarkPresenter,
    private val navController: NavController
) : ListAdapter<Cocktail, CheckmarkAdapter.CheckmarkViewHolder>(CheckmarkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckmarkViewHolder {
        val binding = ItemCocktailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CheckmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CheckmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CheckmarkViewHolder(
        private val binding: ItemCocktailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cocktail: Cocktail) {
            binding.apply {
                tvCocktailName.text = cocktail.strDrink
                tvCocktailCategory.text = cocktail.strCategory

                // Load image
                ImageLoader.loadImage(
                    url = cocktail.strDrinkThumb,
                    imageView = ivCocktail,
                    placeholderResId = R.drawable.imgstart
                )

                // Set up checkmark button - in checkmarks screen, all items are already checkmarked
                llRating.setOnClickListener {
                    // Remove from checkmarks when clicked in the checkmarks screen
                    presenter.removeFromCheckmarks(cocktail)
                }

                // Change text from rating to "Remove" for clarity
                tvRating.text = root.context.getString(R.string.remove_button)

                // Set click listener to navigate to detail
                root.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("cocktail_id", cocktail.idDrink)
                        putString("cocktail_name", cocktail.strDrink)
                        putString("cocktail_category", cocktail.strCategory ?: "")
                        putString("cocktail_alcoholic", cocktail.strAlcoholic ?: "")
                        putString("cocktail_glass", cocktail.strGlass ?: "")
                        putString("cocktail_instructions", cocktail.strInstructions ?: "")
                        putString("cocktail_image", cocktail.strDrinkThumb ?: "")
                        // Handle ingredients and measures safely
                        val ingredients = cocktail.ingredients?.toTypedArray() ?: emptyArray()
                        val measures = cocktail.measures?.toTypedArray() ?: emptyArray()
                        putStringArray("cocktail_ingredients", ingredients)
                        putStringArray("cocktail_measures", measures)
                    }
                    navController.navigate(R.id.navigation_cocktail_detail, bundle)
                }
            }
        }
    }

    private class CheckmarkDiffCallback : DiffUtil.ItemCallback<Cocktail>() {
        override fun areItemsTheSame(oldItem: Cocktail, newItem: Cocktail): Boolean {
            return oldItem.idDrink == newItem.idDrink
        }

        override fun areContentsTheSame(oldItem: Cocktail, newItem: Cocktail): Boolean {
            return oldItem == newItem
        }
    }
} 



