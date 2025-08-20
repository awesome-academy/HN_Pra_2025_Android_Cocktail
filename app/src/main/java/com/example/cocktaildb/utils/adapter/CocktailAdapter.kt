package com.example.cocktaildb.utils.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.ItemCocktailBinding
import com.example.cocktaildb.databinding.ItemCocktailSearchBinding
import com.example.cocktaildb.utils.ImageLoader
import kotlin.random.Random

class CocktailAdapter(
    private var items: List<Cocktail>,
    private val onCocktailClick: (Cocktail) -> Unit,
    private val useSearchLayout: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_SEARCH = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (useSearchLayout) VIEW_TYPE_SEARCH else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SEARCH -> {
                val binding = ItemCocktailSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SearchViewHolder(binding)
            }
            else -> {
                val binding = ItemCocktailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                NormalViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is SearchViewHolder -> holder.bind(item)
            is NormalViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<Cocktail>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class NormalViewHolder(private val binding: ItemCocktailBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(cocktail: Cocktail) {
            binding.tvCocktailName.text = cocktail.strDrink
            binding.tvCocktailCategory.text = cocktail.strCategory ?: "Cocktail"
            
            // Generate random rating between 4.0 and 5.0 if not available
            val rating = cocktail.rating ?: (Random.nextDouble(4.0, 5.0) * 10).toInt() / 10.0f
            binding.tvRating.text = rating.toString()

            // Load image if available
            ImageLoader.loadImage(cocktail.strDrinkThumb, binding.ivCocktail, R.drawable.imgstart)

            itemView.setOnClickListener {
                Log.e("CocktailAdapter", "Item clicked: ${cocktail.strDrink} (${cocktail.idDrink})")
                onCocktailClick(cocktail)
            }
        }
    }

    inner class SearchViewHolder(private val binding: ItemCocktailSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(cocktail: Cocktail) {
            binding.tvName.text = cocktail.strDrink
            binding.tvCategory.text = cocktail.strCategory ?: "Unknown"

            if (cocktail.rating != null) {
                binding.tvRating.text = String.format("%.1f", cocktail.rating)
                binding.tvRating.parent?.let {
                    (it as? View)?.visibility = View.VISIBLE
                }
            } else {
                binding.tvRating.parent?.let {
                    (it as? View)?.visibility = View.GONE
                }
            }

            // Load image
            ImageLoader.loadImage(cocktail.strDrinkThumb, binding.ivThumb, R.drawable.imgstart)

            itemView.setOnClickListener {
                Log.e("CocktailAdapter", "Search Item clicked: ${cocktail.strDrink} (${cocktail.idDrink})")
                onCocktailClick(cocktail)
            }
        }
    }
}

