package com.example.cocktaildb.screen.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.FragmentCocktailDetailBinding
import com.example.cocktaildb.screen.search.SearchActivity
import com.example.cocktaildb.utils.ImageLoader

class CocktailDetailFragment : Fragment() {

    private var _binding: FragmentCocktailDetailBinding? = null
    private val binding get() = _binding!!
    private var cocktail: Cocktail? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCocktailDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        loadCocktailData()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            // Check if we're in SearchActivity context
            if (activity is SearchActivity) {
                // If in SearchActivity, trigger back press
                activity?.onBackPressedDispatcher?.onBackPressed()
            } else {
                // If in MainActivity, use Navigation Component
                findNavController().navigateUp()
            }
        }
    }

    private fun loadCocktailData() {
        // Get data from arguments using Navigation component's safe args
        val args = arguments
        val cocktailName = args?.getString("cocktail_name") ?: "Cocktail"
        val cocktailCategory = args?.getString("cocktail_category") ?: "Cocktail"
        val alcoholic = args?.getString("cocktail_alcoholic") ?: ""
        val glass = args?.getString("cocktail_glass") ?: ""
        val instructions = args?.getString("cocktail_instructions") ?: "No instructions available"
        val imageUrl = args?.getString("cocktail_image")
        val ingredients = args?.getStringArray("cocktail_ingredients") ?: emptyArray()
        val measures = args?.getStringArray("cocktail_measures") ?: emptyArray()

        // Create cocktail object and add to history
        val cocktail = createCocktailFromArgs(
            cocktailName, cocktailCategory, alcoholic, glass, 
            instructions, imageUrl, ingredients, measures
        )
        // Note: History is already added when navigating from other screens
        // This prevents duplicate entries

        // Set cocktail name
        binding.tvCocktailName.text = cocktailName

        // Set description with category, alcoholic status, and glass type
        val description = buildString {
            append("A delicious $cocktailCategory cocktail")
            if (alcoholic.isNotEmpty()) {
                append(" ($alcoholic)")
            }
            if (glass.isNotEmpty()) {
                append(" served in a $glass")
            }
            append(" with carefully selected ingredients.")
        }
        binding.tvDescription.text = description

        // Load image
        ImageLoader.loadImage(imageUrl, binding.ivCocktail, R.drawable.imgstart)

        // Set instructions
        setupInstructions(instructions)

        // Set ingredients (only non-null ingredients)
        setupIngredients(ingredients, measures)
    }

    private fun setupInstructions(instructions: String) {
        val instructionLines = instructions.split(". ")
        val instructionsContainer = binding.llInstructions
        
        // Clear existing instructions
        instructionsContainer.removeAllViews()
        
        instructionLines.forEachIndexed { index, instruction ->
            if (instruction.isNotBlank()) {
                val instructionView = TextView(requireContext()).apply {
                    text = "${index + 1}. $instruction"
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.dark_gray, null))
                    setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.dp_8))
                }
                instructionsContainer.addView(instructionView)
            }
        }
    }

    private fun setupIngredients(ingredients: Array<String>, measures: Array<String>) {
        val ingredientsContainer = binding.llIngredients
        
        // Clear existing ingredients
        ingredientsContainer.removeAllViews()
        
        // Filter out null/empty ingredients
        val validIngredients = ingredients.filterIndexed { index, ingredient ->
            ingredient.isNotBlank() && ingredient != "null"
        }
        
        // Update ingredients count in header
        val ingredientsCount = validIngredients.size
        binding.tvIngredientsHeader.text = getString(R.string.ingredients) + " ($ingredientsCount)"
        
        validIngredients.forEachIndexed { index, ingredient ->
            val ingredientView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_ingredient, ingredientsContainer, false)
            
            val ingredientName = ingredientView.findViewById<TextView>(R.id.tvIngredientName)
            val ingredientMeasure = ingredientView.findViewById<TextView>(R.id.tvIngredientMeasure)
            
            ingredientName.text = ingredient
            
            // Find corresponding measure
            val originalIndex = ingredients.indexOf(ingredient)
            val measure = if (originalIndex < measures.size && originalIndex >= 0) {
                measures[originalIndex]?.takeIf { it.isNotBlank() && it != "null" } ?: ""
            } else ""
            
            ingredientMeasure.text = measure
            
            ingredientsContainer.addView(ingredientView)
        }
    }

    private fun setupClickListeners() {
        binding.btnBookmark.setOnClickListener {
            // TODO: Implement bookmark functionality
        }
        
        binding.btnFavorite.setOnClickListener {
            // TODO: Implement favorite functionality
        }
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
    ): com.example.cocktaildb.data.model.Cocktail {
        return com.example.cocktaildb.data.model.Cocktail(
            idDrink = arguments?.getString("cocktail_id") ?: "",
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

    private fun addToHistory(cocktail: com.example.cocktaildb.data.model.Cocktail) {
        try {
            // Use HistoryPresenter companion method to add to history
            com.example.cocktaildb.screen.history.HistoryPresenter.addToHistory(requireContext(), cocktail)
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

} 