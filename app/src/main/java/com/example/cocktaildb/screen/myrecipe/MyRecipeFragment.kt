package com.example.cocktaildb.screen.myrecipe

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.databinding.FragmentMyRecipeBinding
import com.example.cocktaildb.databinding.ItemCocktailBinding
import com.example.cocktaildb.utils.base.BaseFragment

/**
 * Fragment displaying user's created recipes
 */
class MyRecipeFragment : BaseFragment<FragmentMyRecipeBinding>(), MyRecipeContract.View {

    private lateinit var presenter: MyRecipePresenter
    private lateinit var recipeAdapter: RecipeAdapter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentMyRecipeBinding {
        return FragmentMyRecipeBinding.inflate(inflater)
    }

    override fun initView() {
        // Setup RecyclerView
        recipeAdapter = RecipeAdapter()

        // Apply proper item spacing decoration
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.dp_8)
        viewBinding.recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                // Apply spacing to all items
                outRect.left = spacingInPixels
                outRect.right = spacingInPixels
                outRect.bottom = spacingInPixels

                // Determine if this is an item in the left or right column
                val isLeftColumn = position % 2 == 0

                // Add more space on the left for left column items and on the right for right column items
                if (isLeftColumn) {
                    outRect.left = spacingInPixels * 2
                } else {
                    outRect.right = spacingInPixels * 2
                }

                // Add top margin only for the first row items
                if (position == 0 || position == 1) {
                    outRect.top = spacingInPixels
                }
            }
        })

        viewBinding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2) // Display 2 items per row
            adapter = recipeAdapter
            clipToPadding = false  // Allow scrolling into the padding area
            setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_nav_height))
        }
    }

    override fun initData() {
        // Initialize presenter with repository
        val dataSource = CocktailLocalDataSource()
        val repository = CocktailRepository(dataSource)
        presenter = MyRecipePresenter(repository)
        presenter.setView(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.onStart()
    }

    override fun onPause() {
        presenter.onStop()
        super.onPause()
    }

    override fun showUserRecipes(cocktails: List<Cocktail>) {
        recipeAdapter.setRecipes(cocktails)
    }

    override fun displayLoading(show: Boolean) {
        viewBinding.loadingView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun displayError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Adapter for the recipes RecyclerView
     */
    private inner class RecipeAdapter : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

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

        inner class RecipeViewHolder(private val binding: ItemCocktailBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(cocktail: Cocktail) {
                binding.tvCocktailName.text = cocktail.strDrink
                binding.tvCocktailCategory.text = cocktail.strCategory ?: getString(R.string.custom_recipe)

                // Load the cocktail image using our custom ImageLoader
                com.example.cocktaildb.utils.ImageLoader.loadImage(
                    url = cocktail.strDrinkThumb,
                    imageView = binding.ivCocktail,
                    placeholderResId = R.drawable.ic_launcher_background
                )

                // Set rating for my recipes
                binding.tvRating.text = getString(R.string.my_recipe)
            }
        }
    }

    companion object {
        fun newInstance() = MyRecipeFragment()
    }
}
