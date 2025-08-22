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
            updateFavoriteButtonState(it)
            presenter.checkBookmarkStatus(it.idDrink)
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
                    activity?.runOnUiThread { cocktail?.let { updateFavoriteButtonState(it) } }
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

        // Only add to history if not coming from MyRecipe
        val isFromMyRecipe = args?.getBoolean(KEY_FROM_MY_RECIPE, false) ?: false
        if (!isFromMyRecipe) {
            try {
                HistoryPresenter.addToHistory(requireContext(), cocktail)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to history", e)
            }
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
        setupIngredients(ingredients, measures)
        presenter.loadRelatedCocktails(cocktailName, cocktailCategory)
        if (cocktailId.isNotEmpty() && (ingredients.isEmpty() || ingredients.all { it.isEmpty() })) {
            loadRecipeIngredientsFromFirebase(cocktailId)
        } else {
            setupIngredients(ingredients, measures)
        }
    }

    private fun setupInstructions(instructions: String) {
        val instructionLines = instructions.split(". ")
        val instructionsContainer = viewBinding.llInstructions
        instructionsContainer.removeAllViews()
        instructionLines.forEachIndexed { index, instruction ->
            if (instruction.isNotBlank()) {
                val instructionView = TextView(requireContext()).apply {
                    text = getString(R.string.instruction_numbered, index + 1, instruction)
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.dark_gray, null))
                    setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.dp_8))
                }
                instructionsContainer.addView(instructionView)
            }
        }
    }

    private fun setupIngredients(ingredients: Array<String>, measures: Array<String>) {
        val ingredientsContainer = viewBinding.llIngredients
        ingredientsContainer.removeAllViews()
        val validIngredients = ingredients.filterIndexed { index, ingredient ->
            ingredient.isNotBlank() && ingredient != "null"
        }
        val ingredientsCount = validIngredients.size
        viewBinding.tvIngredientsHeader.text = getString(R.string.ingredients_count, ingredientsCount)
        validIngredients.forEachIndexed { index, ingredient ->
            val ingredientView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_ingredient, ingredientsContainer, false)
            val ingredientName = ingredientView.findViewById<TextView>(R.id.tvIngredientName)
            val ingredientMeasure = ingredientView.findViewById<TextView>(R.id.tvIngredientMeasure)
            ingredientName.text = ingredient
            val originalIndex = ingredients.indexOf(ingredient)
            val measure = if (originalIndex < measures.size && originalIndex >= 0) {
                measures[originalIndex]?.takeIf { it.isNotBlank() && it != "null" } ?: ""
            } else ""
            ingredientMeasure.text = measure
            ingredientsContainer.addView(ingredientView)
        }
    }

    private fun loadRecipeIngredientsFromFirebase(recipeId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    com.example.cocktaildb.data.service.RecipeFirebaseService().getRecipeIngredients(recipeId)
                }
                result.fold(
                    onSuccess = { ingredients ->
                        Log.d(TAG, "Loaded ${ingredients.size} ingredients for recipe $recipeId")
                        if (ingredients.isNotEmpty()) {
                            val ingredientNames = ingredients.map { it.ingredientName }.toTypedArray()
                            val ingredientMeasures = ingredients.map { "${it.quantity} ${it.unit}".trim() }.toTypedArray()
                            setupIngredients(ingredientNames, ingredientMeasures)
                            // Update history with enriched ingredients for offline access
                            // Only update history if not coming from MyRecipe
                            val isFromMyRecipe = arguments?.getBoolean(KEY_FROM_MY_RECIPE, false) ?: false
                            if (!isFromMyRecipe) {
                                try {
                                    val updated = this@CocktailDetailFragment.cocktail?.copy(
                                        ingredients = ingredientNames.toList(),
                                        measures = ingredientMeasures.toList()
                                    )
                                    if (updated != null) {
                                        this@CocktailDetailFragment.cocktail = updated
                                        HistoryPresenter.addToHistory(requireContext(), updated)
                                    }
                                } catch (_: Exception) { }
                            } else {
                                // Still update cocktail object, just don't save to history
                                val updated = this@CocktailDetailFragment.cocktail?.copy(
                                    ingredients = ingredientNames.toList(),
                                    measures = ingredientMeasures.toList()
                                )
                                if (updated != null) {
                                    this@CocktailDetailFragment.cocktail = updated
                                }
                            }
                        } else {
                            setupIngredients(emptyArray(), emptyArray())
                        }
                    },
                    onFailure = { _ ->
                        setupIngredients(emptyArray(), emptyArray())
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ingredients for recipe $recipeId", e)
                setupIngredients(emptyArray(), emptyArray())
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBookmark.setOnClickListener {
            isUserBookmarkAction = true
            cocktail?.let { presenter.toggleBookmark(it) }
        }
        val currentCocktail = createCocktailFromArgs(
            arguments?.getString(KEY_COCKTAIL_NAME) ?: "",
            arguments?.getString(KEY_COCKTAIL_CATEGORY) ?: "",
            arguments?.getString(KEY_COCKTAIL_ALCOHOLIC) ?: "",
            arguments?.getString(KEY_COCKTAIL_GLASS) ?: "",
            arguments?.getString(KEY_COCKTAIL_INSTRUCTIONS) ?: "",
            arguments?.getString(KEY_COCKTAIL_IMAGE),
            arguments?.getStringArray(KEY_COCKTAIL_INGREDIENTS) ?: emptyArray(),
            arguments?.getStringArray(KEY_COCKTAIL_MEASURES) ?: emptyArray()
        )
        this.cocktail = currentCocktail
        updateFavoriteButtonState(currentCocktail)
        viewBinding.btnFavorite.setOnClickListener {
            viewBinding.btnFavorite.isEnabled = false
            FavoritesManager.toggleFavoriteOfflineAware(requireContext(), currentCocktail) { isFavorite ->
                activity?.runOnUiThread {
                    viewBinding.btnFavorite.isEnabled = true
                    if (isFavorite) {
                        viewBinding.btnFavorite.setColorFilter(resources.getColor(R.color.pink_primary, null))
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_added_cocktail_to_favorites, currentCocktail.strDrink),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        viewBinding.btnFavorite.setColorFilter(resources.getColor(R.color.red, null))
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_removed_cocktail_from_favorites, currentCocktail.strDrink),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
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

    override fun updateFavoriteButtonState(isFavorite: Boolean) {
        // Implementation will be handled by updateFavoriteButtonState(cocktail) method
    }

    override fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorResource(resourceId: Int) {
        showError(getString(resourceId))
    }

    private fun updateBookmarkUI(isBookmarked: Boolean) {
        if (isBookmarked) {
            viewBinding.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            viewBinding.btnBookmark.setColorFilter(resources.getColor(R.color.pink_primary, null))
            if (isUserBookmarkAction) {
                cocktail?.let { currentCocktail ->
                    Toast.makeText(
                        requireContext(),
                        "Added ${currentCocktail.strDrink} to bookmarks",
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
                        "Removed ${currentCocktail.strDrink} from bookmarks",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                isUserBookmarkAction = false
            }
        }
    }

    private fun updateFavoriteButtonState(cocktail: Cocktail) {
        val isFavorite = FavoritesManager.isFavorite(cocktail.idDrink)
        if (isFavorite) {
            viewBinding.btnFavorite.setColorFilter(resources.getColor(R.color.pink_primary, null))
        } else {
            viewBinding.btnFavorite.setColorFilter(resources.getColor(R.color.red, null))
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
