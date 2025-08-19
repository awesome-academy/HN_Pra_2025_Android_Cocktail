package com.example.cocktaildb.screen.myrecipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.data.model.RecipeImage
import com.example.cocktaildb.databinding.ItemCocktailCardBinding
import com.example.cocktaildb.utils.ImageLoader

class RecipeAdapter : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private val recipes: MutableList<Recipe> = mutableListOf()
    private val recipeImages: MutableMap<String, RecipeImage?> = mutableMapOf()
    private var onItemClickListener: ((Recipe) -> Unit)? = null

    fun setRecipes(recipes: Collection<Recipe>) {
        this.recipes.clear()
        this.recipes.addAll(recipes)
        notifyDataSetChanged()
    }

    fun setRecipeImage(recipeId: String, image: RecipeImage?) {
        recipeImages[recipeId] = image
        val position = recipes.indexOfFirst { it.id == recipeId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun setOnItemClickListener(listener: (Recipe) -> Unit) {
        onItemClickListener = listener
    }

    fun getRecipeImageUrl(recipeId: String): String? {
        return recipeImages[recipeId]?.imageUrl
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemCocktailCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe, recipeImages[recipe.id])

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(recipe)
        }
    }

    override fun getItemCount(): Int = recipes.size

    class RecipeViewHolder(private val binding: ItemCocktailCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: Recipe, image: RecipeImage?) {
            binding.cocktailTitleTextView.text = recipe.name
            binding.cocktailCategoryTextView.text = recipe.category.ifEmpty { 
                binding.root.context.getString(R.string.custom_recipe)
            }

            val imageUrl = image?.imageUrl ?: ""
            println("RecipeAdapter: Loading image for recipe ${recipe.name}, URL: $imageUrl")
            
            if (imageUrl.isNotEmpty()) {
                try {
                    ImageLoader.loadImage(
                        url = imageUrl,
                        imageView = binding.cocktailImageView,
                        placeholderResId = R.mipmap.chocolate_milk
                    )
                } catch (e: Exception) {
                    println("RecipeAdapter: Error loading image: ${e.message}")
                    binding.cocktailImageView.setImageResource(R.mipmap.chocolate_milk)
                }
            } else {
                println("RecipeAdapter: No image URL, using placeholder")
                binding.cocktailImageView.setImageResource(R.mipmap.chocolate_milk)
            }
            binding.ratingChip.text = binding.root.context.getString(R.string.my_recipe)
        }
    }
}

