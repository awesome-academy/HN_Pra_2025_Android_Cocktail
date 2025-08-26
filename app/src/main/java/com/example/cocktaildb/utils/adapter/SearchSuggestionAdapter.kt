package com.example.cocktaildb.utils.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.databinding.ItemSearchSuggestionBinding

class SearchSuggestionAdapter(
    private var suggestions: List<String> = emptyList(),
    private val onSuggestionClick: (String) -> Unit,
    private val onSuggestionRemove: (String) -> Unit
) : RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder>() {

    fun updateSuggestions(newSuggestions: List<String>) {
        val filteredSuggestions = newSuggestions.filter { it.isNotBlank() }
        suggestions = filteredSuggestions
        notifyDataSetChanged()
    }
    
    fun isEmpty(): Boolean = suggestions.isEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ItemSearchSuggestionBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int = suggestions.size

    inner class SuggestionViewHolder(
        private val binding: ItemSearchSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(suggestion: String) {
            binding.tvSuggestion.text = suggestion
            
            binding.root.setOnClickListener {
                onSuggestionClick(suggestion)
            }
            
            binding.btnRemove.setOnClickListener {
                onSuggestionRemove(suggestion)
            }
        }
    }
}
