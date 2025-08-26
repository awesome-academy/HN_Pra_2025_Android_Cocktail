package com.example.cocktaildb.screen.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.databinding.FragmentCocktailDetailBinding
import com.example.cocktaildb.screen.search.SearchActivity
import com.example.cocktaildb.utils.ImageLoader
import com.example.cocktaildb.data.manager.FavoritesManager
import com.example.cocktaildb.utils.adapter.RelatedCocktailAdapter
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.screen.history.HistoryPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CocktailDetailFragment : BaseFragment<FragmentCocktailDetailBinding>(), CocktailDetailContract.View {
    companion object {
        const val KEY_COCKTAIL_ID = "cocktail_id"
        const val KEY_COCKTAIL_NAME = "cocktail_name"
        const val KEY_COCKTAIL_CATEGORY = "cocktail_category"
        const val KEY_COCKTAIL_ALCOHOLIC = "cocktail_alcoholic"
        const val KEY_COCKTAIL_GLASS = "cocktail_glass"
        const val KEY_COCKTAIL_INSTRUCTIONS = "cocktail_instructions"
        const val KEY_COCKTAIL_IMAGE = "cocktail_image"
        const val KEY_COCKTAIL_INGREDIENTS = "cocktail_ingredients"
        const val KEY_COCKTAIL_MEASURES = "cocktail_measures"
        const val KEY_FROM_TODAY_DRINK = "from_today_drink"
        const val KEY_FROM_MY_RECIPE = "from_my_recipe"
        const val KEY_FROM_SHARED_COCKTAILS = "from_shared_cocktails"
    }

    private val binding get() = viewBinding
    private var cocktail: Cocktail? = null
    private val TAG = "CocktailDetailFragment"
    private lateinit var relatedCocktailAdapter: RelatedCocktailAdapter
    private lateinit var presenter: CocktailDetailPresenter
    private var isUserBookmarkAction = false

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentCocktailDetailBinding {
        return FragmentCocktailDetailBinding.inflate(inflater)
    }

    override fun initView() {
        setupPresenter()
        setupToolbar()
        setupRelatedCocktailsRecyclerView()
        presenter.setView(this)
        setupClickListeners()
        ensureFavoritesLoaded()
        cocktail?.let { presenter.checkBookmarkStatus(it.idDrink) }
    }

    override fun initData() {
        loadCocktailData()
    }

    override fun onResume() {
        super.onResume()
        cocktail?.let {
            presenter.checkBookmarkStatus(it.idDrink)
            presenter.checkFavoriteStatus(it.idDrink) // <-- thêm dòng này
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onStop()
    }

    private fun ensureFavoritesLoaded() {
        if (!FavoritesManager.isInitialized()) {
            Log.d(TAG, "Favorites not initialized, loading them now")
            FavoritesManager.loadFavoritesFromFirestore { success ->
                if (success) {
                    Log.d(TAG, "Favorites loaded successfully")
                    activity?.runOnUiThread {
                        cocktail?.let { presenter.checkFavoriteStatus(it.idDrink) } // <-- dùng presenter
                    }
                } else {
                    Log.e(TAG, "Failed to load favorites")
                }
            }
        } else {
            Log.d(TAG, "Favorites already initialized")
        }
    }


    private fun setupPresenter() {
        val repository = CocktailRepository(CocktailRemoteDataSource())
        val authRepository = com.example.cocktaildb.data.repository.AuthRepository()
        presenter = CocktailDetailPresenter(repository, authRepository)
        presenter.setView(this)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (activity is SearchActivity) {
                val searchActivity = activity as SearchActivity
                if (searchActivity.intent.getBooleanExtra(KEY_FROM_TODAY_DRINK, false)) {
                    searchActivity.finish()
                } else {
                    searchActivity.onBackPressedDispatcher.onBackPressed()
                }
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupRelatedCocktailsRecyclerView() {
        relatedCocktailAdapter = RelatedCocktailAdapter(
            emptyList(),
            { cocktail -> navigateToCocktailDetail(cocktail) }
        )
        binding.rvRelatedCocktails.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = relatedCocktailAdapter
        }
    }

    private fun loadCocktailData() {
        val args = arguments
        val cocktailId = args?.getString(KEY_COCKTAIL_ID) ?: ""
        val cocktailName = args?.getString(KEY_COCKTAIL_NAME) ?: getString(R.string.default_cocktail_name)
        val cocktailCategory = args?.getString(KEY_COCKTAIL_CATEGORY) ?: getString(R.string.default_category_cocktail)
        val alcoholic = args?.getString(KEY_COCKTAIL_ALCOHOLIC) ?: ""
        val glass = args?.getString(KEY_COCKTAIL_GLASS) ?: ""
        val instructions = args?.getString(KEY_COCKTAIL_INSTRUCTIONS) ?: getString(R.string.no_instructions_available)
        val imageUrl = args?.getString(KEY_COCKTAIL_IMAGE)
        val ingredients = args?.getStringArray(KEY_COCKTAIL_INGREDIENTS) ?: emptyArray()
        val measures = args?.getStringArray(KEY_COCKTAIL_MEASURES) ?: emptyArray()

        Log.d(TAG, "Loading cocktail data:")
        Log.d(TAG, "ID: $cocktailId")
        Log.d(TAG, "Name: $cocktailName")
        Log.d(TAG, "Instructions length: ${instructions.length}")
        Log.d(TAG, "Instructions preview: ${instructions.take(100)}")
        Log.d(TAG, "Ingredients count: ${ingredients.size}")
        Log.d(TAG, "Ingredients: ${ingredients.joinToString(", ")}")
        Log.d(TAG, "Measures count: ${measures.size}")
        Log.d(TAG, "Measures: ${measures.joinToString(", ")}")

        val cocktail = Cocktail(
            idDrink = cocktailId,
            strDrink = cocktailName,
            strCategory = cocktailCategory,
            strAlcoholic = alcoholic,
            strGlass = glass,
            strInstructions = instructions,
            strDrinkThumb = imageUrl,
            ingredients = ingredients.toList(),
            measures = measures.toList()
        )
        this.cocktail = cocktail
        val isFromMyRecipe = args?.getBoolean(KEY_FROM_MY_RECIPE, false) ?: false
        val isFromSharedCocktails = args?.getBoolean(KEY_FROM_SHARED_COCKTAILS, false) ?: false
        args?.keySet()?.forEach { key ->
            val value = args.get(key)
            when (key) {
                KEY_COCKTAIL_INSTRUCTIONS -> {
                    Log.e(TAG, "Bundle[$key] = '$value' (length: ${value?.toString()?.length ?: 0})")
                }
                KEY_COCKTAIL_INGREDIENTS -> {
                    val ingredientsArray = value as? Array<*>
                    Log.e(TAG, "Bundle[$key] = ${ingredientsArray?.contentToString() ?: "null"}")
                    ingredientsArray?.forEachIndexed { index, ingredient -> 
                        Log.e(TAG, "  Ingredient[$index] = '$ingredient'")
                    }
                }
                KEY_COCKTAIL_MEASURES -> {
                    val measuresArray = value as? Array<*>
                    Log.e(TAG, "Bundle[$key] = ${measuresArray?.contentToString() ?: "null"}")
                    measuresArray?.forEachIndexed { index, measure -> 
                        Log.e(TAG, "  Measure[$index] = '$measure'")
                    }
                }
                else -> {
                    Log.e(TAG, "Bundle[$key] = $value")
                }
            }
        }
        
        // Additional check for missing instructions
        if (args?.containsKey(KEY_COCKTAIL_INSTRUCTIONS) != true) {
            Log.w(TAG, "WARNING: cocktail_instructions key is missing from bundle!")
        } else {
            val instructionsValue = args.getString(KEY_COCKTAIL_INSTRUCTIONS)
            if (instructionsValue.isNullOrBlank()) {
                Log.w(TAG, "WARNING: cocktail_instructions is null or empty!")
            }
        }
        
        if (!isFromMyRecipe && !isFromSharedCocktails) {
            Log.d(TAG, "Cocktail will be saved to history - not from my recipe and not from shared cocktails")
            try {
                HistoryPresenter.addToHistory(requireContext(), cocktail)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to history", e)
            }
        } else {
            Log.d(TAG, "Cocktail will NOT be saved to history - isFromMyRecipe: $isFromMyRecipe, isFromSharedCocktails: $isFromSharedCocktails")
        }

        viewBinding.tvCocktailName.text = cocktailName
        val descriptionBuilder = StringBuilder()
        descriptionBuilder.append(getString(R.string.desc_cocktail_base, cocktailCategory))
        if (alcoholic.isNotEmpty()) descriptionBuilder.append(getString(R.string.desc_alcoholic_suffix, alcoholic))
        if (glass.isNotEmpty()) descriptionBuilder.append(getString(R.string.desc_glass_suffix, glass))
        descriptionBuilder.append(getString(R.string.desc_tail))
        viewBinding.tvDescription.text = descriptionBuilder.toString()
        ImageLoader.loadImage(imageUrl, viewBinding.ivCocktail, R.drawable.imgstart)
        setupInstructions(instructions)
        presenter.loadRelatedCocktails(cocktailName, cocktailCategory)
        
        // Check if ingredients are empty or need to be loaded from Firebase
        if (cocktailId.isNotEmpty() && (ingredients.isEmpty() || ingredients.all { it.isEmpty() || it == "null" })) {
            Log.d(TAG, "Loading ingredients from Firebase for cocktail: $cocktailId")
            loadRecipeIngredientsFromFirebase(cocktailId)
        } else {
            Log.d(TAG, "Using provided ingredients, count: ${ingredients.size}")
            setupIngredients(ingredients, measures)
        }
        
        // If instructions are missing or empty, try to load from external API
        if (instructions.isBlank() || instructions.equals(getString(R.string.no_instructions_available), ignoreCase = true)) {
            Log.d(TAG, "Instructions are missing, trying to load from external API for cocktail: $cocktailName")
            loadCocktailFromExternalAPI(cocktailName)
        }
    }

    private fun setupInstructions(instructions: String) {
        val instructionsContainer = viewBinding.llInstructions
        instructionsContainer.removeAllViews()
        
        // Check if instructions is empty, null, or contains only whitespace
        if (instructions.isBlank() || instructions.equals("null", ignoreCase = true)) {
            Log.d(TAG, "Instructions are empty or null")
            val noInstructionsView = TextView(requireContext()).apply {
                text = getString(R.string.no_instructions_available)
                textSize = 16f
                setTextColor(resources.getColor(R.color.dark_gray, null))
                setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.dp_8))
            }
            instructionsContainer.addView(noInstructionsView)
            return
        }
        
        val instructionLines = instructions.split(". ", ". ", ".\n", "\n")
        Log.d(TAG, "Setting up instructions, lines count: ${instructionLines.size}")
        
        instructionLines.forEachIndexed { index, instruction ->
            val cleanInstruction = instruction.trim()
            if (cleanInstruction.isNotBlank() && !cleanInstruction.equals("null", ignoreCase = true)) {
                val instructionView = TextView(requireContext()).apply {
                    text = getString(R.string.instruction_numbered, index + 1, cleanInstruction)
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.dark_gray, null))
                    setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.dp_8))
                }
                instructionsContainer.addView(instructionView)
            }
        }
        
        // If no valid instructions were added, show default message
        if (instructionsContainer.childCount == 0) {
            val noInstructionsView = TextView(requireContext()).apply {
                text = getString(R.string.no_instructions_available)
                textSize = 16f
                setTextColor(resources.getColor(R.color.dark_gray, null))
                setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.dp_8))
            }
            instructionsContainer.addView(noInstructionsView)
        }
    }

    private fun setupIngredients(ingredients: Array<String>, measures: Array<String>) {
        val ingredientsContainer = viewBinding.llIngredients
        ingredientsContainer.removeAllViews()
        
        // Filter valid ingredients (not empty, not null, not "null" string)
        val validIngredients = ingredients.filterIndexed { index, ingredient ->
            !ingredient.isNullOrBlank() && !ingredient.equals("null", ignoreCase = true)
        }
        
        val ingredientsCount = validIngredients.size
        viewBinding.tvIngredientsHeader.text = getString(R.string.ingredients_count, ingredientsCount)
        
        Log.d(TAG, "Setting up ingredients, valid count: $ingredientsCount")
        
        if (validIngredients.isEmpty()) {
            val noIngredientsView = TextView(requireContext()).apply {
                text = "No ingredients available"
                textSize = 16f
                setTextColor(resources.getColor(R.color.dark_gray, null))
                setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.dp_8))
            }
            ingredientsContainer.addView(noIngredientsView)
            return
        }
        
        validIngredients.forEachIndexed { index, ingredient ->
            val ingredientView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_ingredient, ingredientsContainer, false)
            val ingredientName = ingredientView.findViewById<TextView>(R.id.tvIngredientName)
            val ingredientMeasure = ingredientView.findViewById<TextView>(R.id.tvIngredientMeasure)
            
            ingredientName.text = ingredient
            
            // Find the original index to get the corresponding measure
            val originalIndex = ingredients.indexOf(ingredient)
            val measure = if (originalIndex < measures.size && originalIndex >= 0) {
                val measureText = measures[originalIndex]
                if (!measureText.isNullOrBlank() && !measureText.equals("null", ignoreCase = true)) {
                    measureText.trim()
                } else {
                    ""
                }
            } else {
                ""
            }
            
            ingredientMeasure.text = measure
            ingredientsContainer.addView(ingredientView)
            
            Log.d(TAG, "Added ingredient: $ingredient, measure: $measure")
        }
    }

    private fun loadRecipeIngredientsFromFirebase(recipeId: String) {
        Log.d(TAG, "Loading ingredients from Firebase for recipe: $recipeId")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    com.example.cocktaildb.data.service.RecipeFirebaseService().getRecipeIngredients(recipeId)
                }
                result.fold(
                    onSuccess = { ingredients ->
                        Log.d(TAG, "Successfully loaded ${ingredients.size} ingredients for recipe $recipeId")
                        if (ingredients.isNotEmpty()) {
                            val ingredientNames = ingredients.map { it.ingredientName }.toTypedArray()
                            val ingredientMeasures = ingredients.map { "${it.quantity} ${it.unit}".trim() }.toTypedArray()
                            
                            Log.d(TAG, "Ingredients: ${ingredientNames.joinToString(", ")}")
                            Log.d(TAG, "Measures: ${ingredientMeasures.joinToString(", ")}")
                            
                            setupIngredients(ingredientNames, ingredientMeasures)
                            
                            // Update cocktail object with enriched ingredients
                            val isFromMyRecipe = arguments?.getBoolean(KEY_FROM_MY_RECIPE, false) ?: false
                            val isFromSharedCocktails = arguments?.getBoolean(KEY_FROM_SHARED_COCKTAILS, false) ?: false
                            val isFromSharedCocktailsHome = arguments?.getBoolean("from_shared_cocktails", false) ?: false
                            
                            val updated = this@CocktailDetailFragment.cocktail?.copy(
                                ingredients = ingredientNames.toList(),
                                measures = ingredientMeasures.toList()
                            )
                            if (updated != null) {
                                this@CocktailDetailFragment.cocktail = updated
                                
                                // Only add to history if not from recipe sources
                                if (!isFromMyRecipe && !isFromSharedCocktails && !isFromSharedCocktailsHome) {
                                    try {
                                        HistoryPresenter.addToHistory(requireContext(), updated)
                                        Log.d(TAG, "Added enriched cocktail to history")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error adding enriched cocktail to history", e)
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "No ingredients found for recipe $recipeId in Firebase, trying external API...")
                            // Try to load from external API if Firebase has no data
                            tryLoadIngredientsFromExternalAPI()
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to load ingredients for recipe $recipeId from Firebase", error)
                        // Try to load from external API if Firebase fails
                        tryLoadIngredientsFromExternalAPI()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading ingredients for recipe $recipeId", e)
                tryLoadIngredientsFromExternalAPI()
            }
        }
    }

    private fun tryLoadIngredientsFromExternalAPI() {
        val cocktailName = arguments?.getString(KEY_COCKTAIL_NAME) ?: ""
        val cocktailId = arguments?.getString(KEY_COCKTAIL_ID) ?: ""
        
        Log.d(TAG, "Trying to load ingredients from external API for: $cocktailName (ID: $cocktailId)")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val repository = CocktailRepository(CocktailRemoteDataSource())
                val result = withContext(Dispatchers.IO) {
                    // Try by ID first, then by name
                    repository.getCocktailById(cocktailId) ?: run {
                        val searchResults = repository.searchCocktails(cocktailName)
                        searchResults.firstOrNull { it.strDrink.equals(cocktailName, ignoreCase = true) }
                            ?: searchResults.firstOrNull()
                    }
                }
                
                if (result != null && result.ingredients.isNotEmpty()) {
                    Log.d(TAG, "Found ingredients from external API: ${result.ingredients}")
                    setupIngredients(
                        result.ingredients.toTypedArray(),
                        result.measures.toTypedArray()
                    )
                    
                    // Update cocktail object
                    this@CocktailDetailFragment.cocktail = this@CocktailDetailFragment.cocktail?.copy(
                        ingredients = result.ingredients,
                        measures = result.measures
                    )
                } else {
                    Log.d(TAG, "No ingredients found from external API either, showing empty state")
                    setupIngredients(emptyArray(), emptyArray())
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ingredients from external API", e)
                setupIngredients(emptyArray(), emptyArray())
            }
        }
    }

    private fun loadCocktailFromExternalAPI(cocktailName: String) {
        Log.d(TAG, "Loading cocktail details from external API for: $cocktailName")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Try to get cocktail details from the repository
                    val repository = CocktailRepository(CocktailRemoteDataSource())
                    val searchResults = repository.searchCocktails(cocktailName)
                    searchResults.firstOrNull { it.strDrink.equals(cocktailName, ignoreCase = true) }
                        ?: searchResults.firstOrNull()
                }
                
                if (result != null) {
                    Log.d(TAG, "Found external cocktail data:")
                    Log.d(TAG, "Instructions: ${result.strInstructions}")
                    Log.d(TAG, "Ingredients: ${result.ingredients}")
                    Log.d(TAG, "Measures: ${result.measures}")
                    
                    // Update UI with external data if available
                    if (!result.strInstructions.isNullOrBlank()) {
                        setupInstructions(result.strInstructions!!)
                        
                        // Update the cocktail object
                        this@CocktailDetailFragment.cocktail = this@CocktailDetailFragment.cocktail?.copy(
                            strInstructions = result.strInstructions
                        )
                    }
                    
                    // If external cocktail has ingredients, use them
                    if (result.ingredients.isNotEmpty()) {
                        setupIngredients(
                            result.ingredients.toTypedArray(),
                            result.measures.toTypedArray()
                        )
                        
                        // Update the cocktail object
                        this@CocktailDetailFragment.cocktail = this@CocktailDetailFragment.cocktail?.copy(
                            ingredients = result.ingredients,
                            measures = result.measures
                        )
                    }
                } else {
                    Log.d(TAG, "No external data found for cocktail: $cocktailName")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cocktail from external API", e)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBookmark.setOnClickListener {
            isUserBookmarkAction = true
            cocktail?.let { presenter.toggleBookmark(it) }
        }

        binding.btnFavorite.setOnClickListener {
            isUserFavoriteAction = true              // <-- để hiện toast khi do user nhấn
            viewBinding.btnFavorite.isEnabled = false
            cocktail?.let { presenter.toggleFavorite(it) } // <-- giao cho presenter
        }
    }


    override fun updateBookmarkButtonState(isBookmarked: Boolean) {
        viewBinding.btnBookmark.isEnabled = true
        if (isBookmarked) {
            viewBinding.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            viewBinding.btnBookmark.setColorFilter(resources.getColor(R.color.pink_primary, null))
            if (isUserBookmarkAction) {
                cocktail?.let { currentCocktail ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.msg_added_cocktail_to_bookmarks, currentCocktail.strDrink),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                isUserBookmarkAction = false
            }
        } else {
            viewBinding.btnBookmark.setImageResource(R.drawable.ic_bookmark)
            viewBinding.btnBookmark.setColorFilter(resources.getColor(R.color.red, null))
            if (isUserBookmarkAction) {
                cocktail?.let { currentCocktail ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.msg_removed_cocktail_from_bookmarks, currentCocktail.strDrink),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                isUserBookmarkAction = false
            }
        }
    }

    override fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorResource(resourceId: Int) {
        showError(getString(resourceId))
    }


    private var isUserFavoriteAction = false

    override fun updateFavoriteButtonState(isFavorite: Boolean) {
        val current = cocktail ?: return
        viewBinding.btnFavorite.isEnabled = true

        if (isFavorite) {
            viewBinding.btnFavorite.setImageResource(R.drawable.ic_heart_filled)
            viewBinding.btnFavorite.setColorFilter(resources.getColor(R.color.pink_primary, null))

            if (isUserFavoriteAction) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.msg_added_cocktail_to_favorites, current.strDrink),
                    Toast.LENGTH_SHORT
                ).show()
                isUserFavoriteAction = false
            }
        } else {
            viewBinding.btnFavorite.setImageResource(R.drawable.ic_heart)
            viewBinding.btnFavorite.setColorFilter(resources.getColor(R.color.red, null))

            if (isUserFavoriteAction) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.msg_removed_cocktail_from_favorites, current.strDrink),
                    Toast.LENGTH_SHORT
                ).show()
                isUserFavoriteAction = false
            }
        }
    }


    private fun navigateToCocktailDetail(cocktail: Cocktail) {
        val args = Bundle().apply {
            putString(KEY_COCKTAIL_ID, cocktail.idDrink)
            putString(KEY_COCKTAIL_NAME, cocktail.strDrink)
            putString(KEY_COCKTAIL_CATEGORY, cocktail.strCategory ?: getString(R.string.default_category_cocktail))
            putString(KEY_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic ?: "")
            putString(KEY_COCKTAIL_GLASS, cocktail.strGlass ?: "")
            putString(KEY_COCKTAIL_IMAGE, cocktail.strDrinkThumb)
            putStringArray(KEY_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
            putStringArray(KEY_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
        }
        findNavController().navigate(R.id.action_cocktailDetailFragment_self, args)
    }

    override fun showRelatedCocktails(cocktails: List<Cocktail>) {
        relatedCocktailAdapter.submit(cocktails)
    }

    private fun createCocktailFromArgs(
        name: String,
        category: String,
        alcoholic: String,
        glass: String,
        instructions: String,
        imageUrl: String?,
        ingredients: Array<String>,
        measures: Array<String>
    ): Cocktail {
        return Cocktail(
            idDrink = arguments?.getString(KEY_COCKTAIL_ID) ?: "",
            strDrink = name,
            strCategory = category,
            strAlcoholic = alcoholic,
            strGlass = glass,
            strInstructions = instructions,
            strDrinkThumb = imageUrl,
            ingredients = ingredients.toList(),
            measures = measures.toList()
        )
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }
}
