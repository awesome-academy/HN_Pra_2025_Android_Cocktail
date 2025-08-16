package com.example.cocktaildb.screen.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.ItemCocktailBinding
import com.example.cocktaildb.databinding.ItemProfileHeaderBinding
import com.example.cocktaildb.utils.ImageLoader


class ProfileAdapter(
    private val headerClickListener: HeaderClickListener,
    private val cocktailClickListener: CocktailClickListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_COCKTAIL = 1

    private var userName: String = ""
    private var userBio: String = ""
    private var profileImageUrl: String? = null
    private val cocktails: MutableList<Cocktail> = mutableListOf()

    fun setUserProfile(userName: String, userBio: String, profileImageUrl: String?) {
        this.userName = userName
        this.userBio = userBio
        this.profileImageUrl = profileImageUrl
        notifyItemChanged(0)
    }

    fun setCocktails(cocktails: List<Cocktail>) {
        this.cocktails.clear()
        this.cocktails.addAll(cocktails)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_COCKTAIL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemProfileHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding, headerClickListener)
            }
            else -> {
                val binding = ItemCocktailBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false)
                CocktailViewHolder(binding, cocktailClickListener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(userName, userBio, profileImageUrl)
            }
            is CocktailViewHolder -> {
                // Position - 1 because header is at position 0
                holder.bind(cocktails[position - 1])
            }
        }
    }

    override fun getItemCount(): Int = if (cocktails.isEmpty()) 1 else cocktails.size + 1


    class HeaderViewHolder(
        private val binding: ItemProfileHeaderBinding,
        private val clickListener: HeaderClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.myRecipeButton.setOnClickListener {
                clickListener.onMyRecipesClicked()
            }

            binding.historyButton.setOnClickListener {
                clickListener.onHistoryClicked()
            }

            binding.logoutButton.setOnClickListener {
                clickListener.onLogoutClicked()
            }
        }

        fun bind(name: String, bio: String, imageUrl: String?) {
            // Ensure we have some text to display
            binding.userName.text = if (name.isNotEmpty()) name else "User"
            binding.userBio.text = if (bio.isNotEmpty()) bio else "Email not available"

            // Use our custom ImageLoader to load the profile image if available
            if (imageUrl != null && imageUrl.isNotEmpty()) {
                try {
                    ImageLoader.loadImage(
                        url = imageUrl,
                        imageView = binding.profileImageView,
                        placeholderResId = R.drawable.profile_placeholder
                    )
                } catch (e: Exception) {
                    // If image loading fails, use placeholder
                    binding.profileImageView.setImageResource(R.drawable.profile_placeholder)
                }
            } else {
                // Use the profile placeholder if no image URL is provided
                binding.profileImageView.setImageResource(R.drawable.profile_placeholder)
            }
        }
    }


    class CocktailViewHolder(
        private val binding: ItemCocktailBinding,
        private val clickListener: CocktailClickListener?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cocktail: Cocktail) {
            binding.tvCocktailName.text = cocktail.strDrink
            binding.tvCocktailCategory.text = cocktail.strCategory ?: "Cocktail"

            // Load the cocktail image using our custom ImageLoader
            try {
                ImageLoader.loadImage(
                    url = cocktail.strDrinkThumb,
                    imageView = binding.ivCocktail,
                    placeholderResId = R.drawable.ic_launcher_background
                )
            } catch (e: Exception) {
                // If image loading fails, use placeholder
                binding.ivCocktail.setImageResource(R.drawable.ic_launcher_background)
            }

            // Set rating
            binding.tvRating.text = "4.8"

            // Set click listener for cocktail item
            itemView.setOnClickListener {
                clickListener?.onCocktailClicked(cocktail)
            }
        }
    }


    interface HeaderClickListener {
        fun onMyRecipesClicked()
        fun onHistoryClicked()
        fun onLogoutClicked()
    }

    interface CocktailClickListener {
        fun onCocktailClicked(cocktail: Cocktail)
    }
}
