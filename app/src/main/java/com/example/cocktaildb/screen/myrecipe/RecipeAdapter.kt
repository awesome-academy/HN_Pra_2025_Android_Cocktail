package com.example.cocktaildb.screen.myrecipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.ItemCocktailBinding
import com.example.cocktaildb.utils.ImageLoader

class RecipeAdapter : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private val recipes: MutableList<Cocktail> = mutableListOf()

    fun setRecipes(recipes: Collection<Cocktail>) {
        this.recipes.clear()
        this.recipes.addAll(recipes)
        notifyDataSetChanged() // Consider using more specific change notifications
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemCocktailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount(): Int = recipes.size

    class RecipeViewHolder(private val binding: ItemCocktailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cocktail: Cocktail) {
            binding.tvCocktailName.text = cocktail.strDrink
            binding.tvCocktailCategory.text = cocktail.strCategory ?:
                binding.root.context.getString(R.string.custom_recipe)

            // Load the cocktail image using our custom ImageLoader
            ImageLoader.loadImage(
                url = cocktail.strDrinkThumb,
                imageView = binding.ivCocktail,
                placeholderResId = R.drawable.ic_launcher_background
            )

            // Set rating for my recipes
            binding.tvRating.text = binding.root.context.getString(R.string.my_recipe)
        }
    }
}
