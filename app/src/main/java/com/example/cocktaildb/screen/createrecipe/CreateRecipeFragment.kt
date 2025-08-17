package com.example.cocktaildb.screen.createrecipe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.IngredientItem
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.databinding.FragmentCreateRecipeBinding
import com.example.cocktaildb.utils.base.BaseFragment

class CreateRecipeFragment : BaseFragment<FragmentCreateRecipeBinding>(), CreateRecipeContract.View {

    private lateinit var presenter: CreateRecipePresenter
    private var isAlcoholic: Boolean = true
    private var selectedImageUri: Uri? = null
    private lateinit var ingredientsAdapter: IngredientsAdapter
    private val ingredients = mutableListOf<IngredientItem>()

    // Use Activity Result API instead of deprecated startActivityForResult
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                viewBinding.layoutUploadPlaceholder.visibility = View.GONE
                viewBinding.imageViewRecipe.visibility = View.VISIBLE

                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(viewBinding.imageViewRecipe)
            }
        }
    }

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentCreateRecipeBinding {
        return FragmentCreateRecipeBinding.inflate(inflater)
    }

    override fun initData() {
        // Initialize presenter
        val dataSource = CocktailLocalDataSource()
        val repository = CocktailRepository(dataSource)
        val authRepository = AuthRepository(requireContext())
        presenter = CreateRecipePresenter(repository, authRepository)
        presenter.setView(this)

        // Setup RecyclerView for ingredients
        setupIngredientsRecyclerView()
    }

    override fun initView() {
        setupClickListeners()
    }

    private fun setupIngredientsRecyclerView() {
        ingredientsAdapter = IngredientsAdapter(
            ingredients,
            onRemoveClick = { position ->
                // Direct removal from the adapter instead of going through presenter
                if (position >= 0 && position < ingredients.size) {
                    ingredients.removeAt(position)
                    ingredientsAdapter.notifyItemRemoved(position)
                }
            }
        )

        viewBinding.recyclerViewIngredients.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ingredientsAdapter
        }
    }

    private fun setupClickListeners() {
        // Image upload
        viewBinding.layoutUploadPlaceholder.setOnClickListener {
            openImagePicker()
        }

        viewBinding.imageViewRecipe.setOnClickListener {
            openImagePicker()
        }

        // Add new ingredient button
        viewBinding.buttonAddNewIngredient.setOnClickListener {
            // Toggle the ingredient input form visibility
            if (viewBinding.layoutNewIngredient.visibility == View.VISIBLE) {
                viewBinding.layoutNewIngredient.visibility = View.GONE
            } else {
                viewBinding.layoutNewIngredient.visibility = View.VISIBLE
            }
        }

        // Add ingredient to list (plus button)
        viewBinding.buttonAddIngredient.setOnClickListener {
            // Get the input values
            val name = viewBinding.editTextIngredientName.text.toString().trim()
            val measure = viewBinding.editTextIngredientQuantity.text.toString().trim()

            if (name.isNotEmpty() && measure.isNotEmpty()) {
                // Add to the ingredients list
                ingredients.add(IngredientItem(name, measure))
                ingredientsAdapter.notifyItemInserted(ingredients.size - 1)

                // Clear the input fields
                viewBinding.editTextIngredientName.text.clear()
                viewBinding.editTextIngredientQuantity.text.clear()

                // Hide the ingredient input form after successfully adding
                viewBinding.layoutNewIngredient.visibility = View.GONE
            } else {
                Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Save recipe button
        viewBinding.buttonSaveRecipe.setOnClickListener {
            saveRecipe()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun saveRecipe() {
        val name = viewBinding.editTextRecipeTitle.text.toString().trim()
        val instructions = viewBinding.editTextPreparation.text.toString().trim()
        val imageUrl = selectedImageUri?.toString() ?: ""
        val category = "Cocktail" // Default category if spinner not available
        val glass = "Cocktail glass" // Default glass if spinner not available

        val ingredientsList = ingredients.map { it.name }
        val measuresList = ingredients.map { it.measure }

        presenter.saveRecipe(
            name = name,
            instructions = instructions,
            imageUrl = imageUrl,
            ingredients = ingredientsList,
            measures = measuresList,
            category = category,
            glass = glass,
            alcoholic = isAlcoholic
        )
    }

    override fun showLoading(show: Boolean) {
        viewBinding.buttonSaveRecipe.isEnabled = !show
        // Add a progress indicator if needed
    }

    override fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToMyRecipes() {
        findNavController().navigate(R.id.navigation_my_recipe)
    }

    override fun addIngredientField() {
        // Method from the Contract, not needed in the new flow
    }

    override fun removeIngredientField(position: Int) {
        if (position >= 0 && position < ingredients.size) {
            ingredients.removeAt(position)
            ingredientsAdapter.notifyItemRemoved(position)
        }
    }
}
