package com.example.cocktaildb.screen.shared

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
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.example.cocktaildb.databinding.FragmentAllCocktailsBinding
import com.example.cocktaildb.utils.adapter.CocktailAdapter
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.screen.detail.CocktailDetailFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllSharedCocktailsFragment : BaseFragment<FragmentAllCocktailsBinding>(), AllSharedCocktailsContract.View {

    companion object {
        private const val TAG = "AllSharedCocktailsFragment"
        private const val KEY_COCKTAIL_ID = "cocktail_id"
        private const val KEY_COCKTAIL_NAME = "cocktail_name"
        private const val KEY_COCKTAIL_CATEGORY = "cocktail_category"
        private const val KEY_COCKTAIL_ALCOHOLIC = "cocktail_alcoholic"
        private const val KEY_COCKTAIL_GLASS = "cocktail_glass"
        private const val KEY_COCKTAIL_INSTRUCTIONS = "cocktail_instructions"
        private const val KEY_COCKTAIL_IMAGE = "cocktail_image"
        private const val KEY_COCKTAIL_INGREDIENTS = "cocktail_ingredients"
        private const val KEY_COCKTAIL_MEASURES = "cocktail_measures"
        private const val KEY_FROM_SHARED_COCKTAILS = "from_shared_cocktails"
    }

    private lateinit var presenter: AllSharedCocktailsPresenter
    private lateinit var cocktailAdapter: CocktailAdapter
    private lateinit var recipeFirebaseService: RecipeFirebaseService
    private var isPresenterInitialized = false

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentAllCocktailsBinding {
        return FragmentAllCocktailsBinding.inflate(inflater)
    }

    override fun initView() {
        // Setup RecyclerView với CocktailAdapter giống Home screen
        cocktailAdapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                navigateToCocktailDetail(cocktail)
            },
            useSearchLayout = true
        )

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
            adapter = cocktailAdapter
            clipToPadding = false  // Allow scrolling into the padding area
            setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_nav_height))
        }
    viewBinding.btnFilter.visibility = View.GONE
    }

    override fun initData() {
        if (!isPresenterInitialized) {
            recipeFirebaseService = RecipeFirebaseService()
            presenter = AllSharedCocktailsPresenter(recipeFirebaseService)
            presenter.setView(this)
            isPresenterInitialized = true
        } else {
            presenter.setView(this)
        }
        if (!hasDataLoaded()) {
            presenter.loadAllSharedRecipes()
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.onStart()
        if (!hasDataLoaded()) {
            presenter.loadAllSharedRecipes()
        }
    }

    private fun hasDataLoaded(): Boolean {
        return ::cocktailAdapter.isInitialized && cocktailAdapter.itemCount > 0
    }

    override fun onPause() {
        presenter.onStop()
        super.onPause()
    }

    override fun onDestroyView() {
        presenter.setView(null)
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (isRemoving || requireActivity().isFinishing) {
            isPresenterInitialized = false
        }
        super.onDestroy()
    }

    override fun showAllSharedRecipes(cocktails: List<Cocktail>) {
        Log.d(TAG, "Received ${cocktails.size} shared cocktails")
        cocktails.forEach { cocktail ->
            Log.d(TAG, "Cocktail - ${cocktail.strDrink} (ID: ${cocktail.idDrink})")
        }

        cocktailAdapter.submit(cocktails)
    }

    override fun displayLoading(show: Boolean) {
        // TODO: Implement loading indicator if needed
        Log.d(TAG, "Loading: $show")
    }

    override fun displayError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun refreshRecipes() {
        if (::presenter.isInitialized) {
            presenter.refreshAllSharedRecipes()
        }
    }

    private fun navigateToCocktailDetail(cocktail: Cocktail) {
        // Get recipe details from Firebase to check ownership
        val authRepository = com.example.cocktaildb.data.repository.AuthRepository()
        val currentUser = authRepository.getCurrentUser()
        
        Log.d(TAG, "Navigating to cocktail detail: ${cocktail.strDrink} (ID: ${cocktail.idDrink})")
        Log.d(TAG, "Current user ID: ${currentUser?.uid}")
        
        // Launch coroutine to get recipe details and check ownership
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                val recipeResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    recipeFirebaseService.getRecipe(cocktail.idDrink)
                }
                
                val isMyRecipe = recipeResult.getOrNull()?.uid == currentUser?.uid
                Log.d(TAG, "Recipe owner: ${recipeResult.getOrNull()?.uid}")
                Log.d(TAG, "Is my recipe: $isMyRecipe")
                
                val bundle = Bundle().apply {
                    putString(KEY_COCKTAIL_ID, cocktail.idDrink)
                    putString(KEY_COCKTAIL_NAME, cocktail.strDrink)
                    putString(KEY_COCKTAIL_CATEGORY, cocktail.strCategory ?: "")
                    putString(KEY_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic ?: "")
                    putString(KEY_COCKTAIL_GLASS, cocktail.strGlass ?: "")
                    putString(KEY_COCKTAIL_INSTRUCTIONS, cocktail.strInstructions ?: "")
                    putString(KEY_COCKTAIL_IMAGE, cocktail.strDrinkThumb ?: "")
                    putStringArray(KEY_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
                    putStringArray(KEY_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
                    
                    // Only set flag if it's actually MY recipe, otherwise allow saving to history
                    if (isMyRecipe) {
                        putBoolean(KEY_FROM_SHARED_COCKTAILS, true)
                        Log.d(TAG, "Set flag to NOT save to history (my recipe)")
                    } else {
                        Log.d(TAG, "Will save to history (other user's recipe)")
                    }
                }
                findNavController().navigate(R.id.navigation_cocktail_detail, bundle)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking recipe ownership", e)
                // Default to treating as other's recipe (save to history)
                val bundle = Bundle().apply {
                    putString(KEY_COCKTAIL_ID, cocktail.idDrink)
                    putString(KEY_COCKTAIL_NAME, cocktail.strDrink)
                    putString(KEY_COCKTAIL_CATEGORY, cocktail.strCategory ?: "")
                    putString(KEY_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic ?: "")
                    putString(KEY_COCKTAIL_GLASS, cocktail.strGlass ?: "")
                    putString(KEY_COCKTAIL_INSTRUCTIONS, cocktail.strInstructions ?: "")
                    putString(KEY_COCKTAIL_IMAGE, cocktail.strDrinkThumb ?: "")
                    putStringArray(KEY_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
                    putStringArray(KEY_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
                }
                findNavController().navigate(R.id.navigation_cocktail_detail, bundle)
            }
        }
    }
} 

    