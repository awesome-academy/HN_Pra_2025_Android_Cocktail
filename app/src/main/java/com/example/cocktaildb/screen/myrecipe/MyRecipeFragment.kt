package com.example.cocktaildb.screen.myrecipe

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Recipe
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.example.cocktaildb.databinding.FragmentMyRecipeBinding
import com.example.cocktaildb.utils.base.BaseFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MyRecipeFragment : BaseFragment<FragmentMyRecipeBinding>(), MyRecipeContract.View {

    private lateinit var presenter: MyRecipePresenter
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var recipeFirebaseService: RecipeFirebaseService

    companion object {
        private const val TAG = "MyRecipeFragment"
        fun newInstance() = MyRecipeFragment()
    }

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentMyRecipeBinding {
        return FragmentMyRecipeBinding.inflate(inflater)
    }

    override fun initView() {
        // Setup RecyclerView
        recipeAdapter = RecipeAdapter()

        recipeAdapter.setOnItemClickListener { recipe ->
            navigateToRecipeDetail(recipe)
        }

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
        recipeFirebaseService = RecipeFirebaseService()
        val authRepository = AuthRepository()
        presenter = MyRecipePresenter(recipeFirebaseService, authRepository)
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

    override fun showUserRecipes(recipes: List<Recipe>) {
        Log.d(TAG, "Received ${recipes.size} recipes")
        recipes.forEach { recipe ->
            Log.d(TAG, "Recipe - ${recipe.name} (ID: ${recipe.id})")
        }
        
        recipeAdapter.setRecipes(recipes)
        loadRecipeImages(recipes)
    }

    private fun loadRecipeImages(recipes: List<Recipe>) {
        Log.d(TAG, "Starting to load images for ${recipes.size} recipes")
        CoroutineScope(Dispatchers.Main).launch {
            recipes.forEach { recipe ->
                try {
                    Log.d(TAG, "Loading images for recipe ${recipe.id}")
                    val result = withContext(Dispatchers.IO) {
                        recipeFirebaseService.getRecipeImages(recipe.id)
                    }
                    
                    result.fold(
                        onSuccess = { images ->
                            Log.d(TAG, "Found ${images.size} images for recipe ${recipe.id}")
                            val primaryImage = images.find { it.isPrimary } ?: images.firstOrNull()
                            if (primaryImage != null) {
                                Log.d(TAG, "Setting image URL: ${primaryImage.imageUrl}")
                            } else {
                                Log.d(TAG, "No images found for recipe ${recipe.id}")
                            }
                            recipeAdapter.setRecipeImage(recipe.id, primaryImage)
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to load images for recipe ${recipe.id}: ${exception.message}")
                            recipeAdapter.setRecipeImage(recipe.id, null)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading images for recipe ${recipe.id}: ${e.message}")
                    recipeAdapter.setRecipeImage(recipe.id, null)
                }
            }
        }
    }

    override fun displayLoading(show: Boolean) {
        viewBinding.loadingView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun displayError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToRecipeDetail(recipe: Recipe) {
        try {
            val bundle = Bundle().apply {
                putString("cocktail_id", recipe.id)
                putString("cocktail_name", recipe.name)
                putString("cocktail_category", recipe.category)
                putString("cocktail_alcoholic", recipe.alcoholic.ifEmpty { "Unknown" })
                putString("cocktail_glass", "Cocktail Glass")
                putString("cocktail_instructions", recipe.instructions)

                val imageUrl = recipeAdapter.getRecipeImageUrl(recipe.id) ?: ""
                putString("cocktail_image", imageUrl)

                putStringArray("cocktail_ingredients", emptyArray())
                putStringArray("cocktail_measures", emptyArray())
            }

            navigateToDetailFragment(bundle)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to detail: ${e.message}")
            Toast.makeText(context, "Error opening recipe details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToDetailFragment(bundle: Bundle) {
        try {
            findNavController().navigate(R.id.navigation_cocktail_detail, bundle)
        } catch (e: Exception) {
            Log.w(TAG, "Navigation failed, trying fragment transaction: ${e.message}")
            try {
                val detailFragment = com.example.cocktaildb.screen.detail.CocktailDetailFragment()
                detailFragment.arguments = bundle
                
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_activity_main, detailFragment)
                    .addToBackStack(null)
                    .commit()
            } catch (ex: Exception) {
                Log.e(TAG, "Fragment transaction failed: ${ex.message}")
                Toast.makeText(context, "Unable to open recipe details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRecipeIngredients(recipeId: String, onComplete: (List<String>, List<String>) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    recipeFirebaseService.getRecipeIngredients(recipeId)
                }
                
                result.fold(
                    onSuccess = { recipeIngredients ->
                        val ingredients = recipeIngredients.map { it.ingredientName }
                        val measures = recipeIngredients.map { "${it.quantity} ${it.unit}".trim() }
                        onComplete(ingredients, measures)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load ingredients for recipe $recipeId: ${exception.message}")
                        onComplete(emptyList(), emptyList())
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ingredients for recipe $recipeId: ${e.message}")
                onComplete(emptyList(), emptyList())
            }
        }
    }
    
}

