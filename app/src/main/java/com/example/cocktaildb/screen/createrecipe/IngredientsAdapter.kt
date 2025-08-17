package com.example.cocktaildb.screen.createrecipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.IngredientItem


class IngredientsAdapter(
    private val items: List<IngredientItem>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_recipe_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val tvIngredientName: TextView = itemView.findViewById(R.id.tvIngredientName)
        private val tvIngredientMeasure: TextView = itemView.findViewById(R.id.tvIngredientMeasure)
        private val buttonRemoveIngredient: ImageButton = itemView.findViewById(R.id.buttonRemoveIngredient)

        fun bind(item: IngredientItem, position: Int) {
            // Display ingredient data as text
            tvIngredientName.text = item.name
            tvIngredientMeasure.text = item.measure

            // Setup remove button
            buttonRemoveIngredient.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
}
