package com.example.cocktaildb.screen.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.ItemCocktailBinding
import kotlin.random.Random

class CocktailAdapter(
    private val cocktails: List<Cocktail>,
    private val onCocktailClick: (Cocktail) -> Unit
) : RecyclerView.Adapter<CocktailAdapter.CocktailViewHolder>() {

    inner class CocktailViewHolder(private val binding: ItemCocktailBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(cocktail: Cocktail) {
            binding.tvCocktailName.text = cocktail.strDrink
            binding.tvCocktailCategory.text = cocktail.strCategory ?: "Cocktail"
            
            // Generate random rating between 4.0 and 5.0
            val rating = (Random.nextDouble(4.0, 5.0) * 10).toInt() / 10.0
            binding.tvRating.text = rating.toString()

            itemView.setOnClickListener {
                onCocktailClick(cocktail)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CocktailViewHolder {
        val binding = ItemCocktailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CocktailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CocktailViewHolder, position: Int) {
        holder.bind(cocktails[position])
    }

    override fun getItemCount(): Int = cocktails.size
}


