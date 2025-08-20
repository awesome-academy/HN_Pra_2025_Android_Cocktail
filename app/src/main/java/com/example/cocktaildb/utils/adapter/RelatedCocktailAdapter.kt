package com.example.cocktaildb.utils.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.ItemRelatedCocktailBinding
import com.example.cocktaildb.utils.ImageLoader

class RelatedCocktailAdapter(
    private var items: List<Cocktail>,
    private val onCocktailClick: (Cocktail) -> Unit
) : RecyclerView.Adapter<RelatedCocktailAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRelatedCocktailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<Cocktail>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemRelatedCocktailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cocktail: Cocktail) {
            binding.tvCocktailName.text = cocktail.strDrink
            val defaultCategory = itemView.context.getString(R.string.default_category_cocktail)
            binding.tvCocktailCategory.text = cocktail.strCategory ?: defaultCategory
            ImageLoader.loadImage(cocktail.strDrinkThumb, binding.ivCocktail, R.drawable.imgstart)
            itemView.setOnClickListener {
                onCocktailClick(cocktail)
            }
        }
    }
}

