package com.example.cocktaildb.screen.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.graphics.Rect
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.data.service.RecipeFirebaseService
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.databinding.FragmentHomeBinding
import com.example.cocktaildb.screen.search.SearchActivity
import com.example.cocktaildb.utils.adapter.CocktailAdapter
import com.example.cocktaildb.utils.base.BaseFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.math.min

class HomeFragment : BaseFragment<FragmentHomeBinding>(), HomeContract.View {

    companion object {
        private const val TAG = "HomeFragment"
        private const val EXTRA_COCKTAIL_ID = "cocktail_id"
        private const val EXTRA_COCKTAIL_NAME = "cocktail_name"
        private const val EXTRA_COCKTAIL_CATEGORY = "cocktail_category"
        private const val EXTRA_COCKTAIL_ALCOHOLIC = "cocktail_alcoholic"
        private const val EXTRA_COCKTAIL_GLASS = "cocktail_glass"
        private const val EXTRA_COCKTAIL_INSTRUCTIONS = "cocktail_instructions"
        private const val EXTRA_COCKTAIL_IMAGE = "cocktail_image"
        private const val EXTRA_COCKTAIL_INGREDIENTS = "cocktail_ingredients"
        private const val EXTRA_COCKTAIL_MEASURES = "cocktail_measures"
        private const val EXTRA_FROM_SHARED_COCKTAILS = "from_shared_cocktails"
    }

    private lateinit var presenter: HomePresenter
    private lateinit var cocktailAdapter: CocktailAdapter
    private lateinit var shareAdapter: CocktailAdapter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater)
    }

    override fun initView() {
        // Initialize presenter with dependency injection
        val cocktailRepository = CocktailRepository(CocktailRemoteDataSource())
        val authRepository = AuthRepository(requireContext())
        val historyFirebaseService = HistoryFirebaseService()
        val recipeFirebaseService = RecipeFirebaseService()

        presenter = HomePresenter(cocktailRepository, authRepository, historyFirebaseService, recipeFirebaseService)
        presenter.setView(this)

        // Initialize RecyclerView
        setupRecyclerView()

        // Set up search card click listener
        viewBinding.cardSearch.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }
        viewBinding.tvViewAll.setOnClickListener {
            findNavController().navigate(R.id.navigation_all_cocktails)
        }

        // Share: View All click
        viewBinding.tvViewAllShare.setOnClickListener {
            findNavController().navigate(R.id.navigation_all_shared_cocktails)
        }
    }

    private fun setupRecyclerView() {
        cocktailAdapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                Log.e("HomeFragment", "onCocktailClick: ${cocktail.strDrink} (${cocktail.idDrink})")
                presenter.onCocktailClicked(cocktail)
            }
        )

        // Set up RecyclerView with GridLayoutManager showing 2 items per row
        val layoutManager = GridLayoutManager(context, 2)

        // Apply proper item spacing decoration
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.dp_8)
        viewBinding.recyclerViewCocktails.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

        viewBinding.recyclerViewCocktails.apply {
            this.layoutManager = layoutManager
            adapter = cocktailAdapter
            clipToPadding = false  // Allow scrolling into the padding area
        }

        // Share RecyclerView
        shareAdapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                navigateToSharedCocktailDetail(cocktail)
            }
        )

        // Apply same spacing decoration for shared list
        val shareSpacing = resources.getDimensionPixelSize(R.dimen.dp_8)
        viewBinding.recyclerViewShare.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                outRect.left = shareSpacing
                outRect.right = shareSpacing
                outRect.bottom = shareSpacing

                val isLeftColumn = position % 2 == 0
                if (isLeftColumn) {
                    outRect.left = shareSpacing * 2
                } else {
                    outRect.right = shareSpacing * 2
                }

                if (position == 0 || position == 1) {
                    outRect.top = shareSpacing
                }
            }
        })

        viewBinding.recyclerViewShare.apply {
            this.layoutManager = GridLayoutManager(context, 2)
            adapter = shareAdapter
            clipToPadding = false
        }
    }

    override fun initData() {
        presenter.onStart()
        presenter.loadCocktails()
        presenter.loadSharedCocktails()
    }

    override fun navigateToCocktailDetail(cocktail: Cocktail) {
        // Navigate to cocktail detail fragment using Navigation Component
        val bundle = Bundle().apply {
            putString(EXTRA_COCKTAIL_ID, cocktail.idDrink)
            putString(EXTRA_COCKTAIL_NAME, cocktail.strDrink)
            putString(EXTRA_COCKTAIL_CATEGORY, cocktail.strCategory ?: "")
            putString(EXTRA_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic ?: "")
            putString(EXTRA_COCKTAIL_GLASS, cocktail.strGlass ?: "")
            putString(EXTRA_COCKTAIL_INSTRUCTIONS, cocktail.strInstructions ?: "")
            putString(EXTRA_COCKTAIL_IMAGE, cocktail.strDrinkThumb ?: "")
            putStringArray(EXTRA_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
            putStringArray(EXTRA_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
        }
        findNavController().navigate(R.id.navigation_cocktail_detail, bundle)
    }

    private fun navigateToSharedCocktailDetail(cocktail: Cocktail) {
        // Get recipe details from Firebase to check ownership
        val authRepository = AuthRepository()
        val currentUser = authRepository.getCurrentUser()
        
        Log.d(TAG, "Navigating to shared cocktail detail: ${cocktail.strDrink} (ID: ${cocktail.idDrink})")
        Log.d(TAG, "Current user ID: ${currentUser?.uid}")
        
        // Launch coroutine to get recipe details and check ownership
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val recipeFirebaseService = com.example.cocktaildb.data.service.RecipeFirebaseService()
                val recipeResult = withContext(Dispatchers.IO) {
                    recipeFirebaseService.getRecipe(cocktail.idDrink)
                }
                
                val isMyRecipe = recipeResult.getOrNull()?.uid == currentUser?.uid
                Log.d(TAG, "Recipe owner: ${recipeResult.getOrNull()?.uid}")
                Log.d(TAG, "Is my recipe: $isMyRecipe")
                
                val bundle = Bundle().apply {
                    putString(EXTRA_COCKTAIL_ID, cocktail.idDrink)
                    putString(EXTRA_COCKTAIL_NAME, cocktail.strDrink)
                    putString(EXTRA_COCKTAIL_CATEGORY, cocktail.strCategory ?: "")
                    putString(EXTRA_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic ?: "")
                    putString(EXTRA_COCKTAIL_GLASS, cocktail.strGlass ?: "")
                    putString(EXTRA_COCKTAIL_INSTRUCTIONS, cocktail.strInstructions ?: "")
                    putString(EXTRA_COCKTAIL_IMAGE, cocktail.strDrinkThumb ?: "")
                    putStringArray(EXTRA_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
                    putStringArray(EXTRA_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
                    
                    // Only set flag if it's actually MY recipe, otherwise allow saving to history
                    if (isMyRecipe) {
                        putBoolean(EXTRA_FROM_SHARED_COCKTAILS, true)
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
                    putString(EXTRA_COCKTAIL_ID, cocktail.idDrink)
                    putString(EXTRA_COCKTAIL_NAME, cocktail.strDrink)
                    putString(EXTRA_COCKTAIL_CATEGORY, cocktail.strCategory ?: "")
                    putString(EXTRA_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic ?: "")
                    putString(EXTRA_COCKTAIL_GLASS, cocktail.strGlass ?: "")
                    putString(EXTRA_COCKTAIL_INSTRUCTIONS, cocktail.strInstructions ?: "")
                    putString(EXTRA_COCKTAIL_IMAGE, cocktail.strDrinkThumb ?: "")
                    putStringArray(EXTRA_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
                    putStringArray(EXTRA_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
                }
                findNavController().navigate(R.id.navigation_cocktail_detail, bundle)
            }
        }
    }

    override fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }

    override fun showCocktails(cocktails: List<Cocktail>) {
        if (cocktails.isEmpty()) {
            showMessage(getString(R.string.msg_no_cocktails_found))
            return
        }

        // Skip first 3 cocktails and show from 4th onwards, limit to 8 cocktails
        val filteredCocktails = if (cocktails.size > 3) {
            val startIndex = 3
            val endIndex = min(startIndex + 8, cocktails.size)
            cocktails.subList(startIndex, endIndex)
        } else {
            // If less than 4 cocktails, show all
            cocktails
        }

        if (filteredCocktails.isEmpty()) {
            showMessage(getString(R.string.msg_no_cocktails_after_filtering))
            return
        }

        // Update adapter with filtered cocktails
        cocktailAdapter.submit(filteredCocktails)

        // Show success message
        showMessage(getString(R.string.msg_loaded_cocktails, filteredCocktails.size))
    }

    override fun showSharedCocktails(cocktails: List<Cocktail>) {
        val limited = if (cocktails.size > 8) cocktails.take(8) else cocktails
        shareAdapter.submit(limited)
    }
}

