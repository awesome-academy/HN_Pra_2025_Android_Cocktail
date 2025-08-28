package com.example.cocktaildb.screen.filter


import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.cocktaildb.R
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.databinding.DialogFilterBinding
import com.example.cocktaildb.utils.base.BaseDialog


class FilterDialog(
    context: Context,
    private val onFilterApplied: (category: String?, alcoholicType: String?) -> Unit,
    private var selectedCategory: String? = null,
    private var selectedAlcoholic: String? = null
) : BaseDialog<DialogFilterBinding>(context), FilterContract.View {


    private lateinit var presenter: FilterPresenter
    private var categories: List<String> = emptyList()
    private var alcoholicTypes: List<String> = emptyList()
    private var tempSelectedCategory: String? = selectedCategory
    private var tempSelectedAlcoholic: String? = selectedAlcoholic


    override fun inflateViewBinding(): DialogFilterBinding {
        return DialogFilterBinding.inflate(layoutInflater)
    }


    override fun initView() {
        window?.setBackgroundDrawableResource(R.color.transparent)
        setupClickListeners()
    }


    override fun initData() {
        initPresenter()


        if (tempSelectedCategory != null) {
            viewBinding.tvSelectedCategory.text = tempSelectedCategory
        } else {
            viewBinding.tvSelectedCategory.text = context.getString(R.string.All_Categories)
        }
        if (tempSelectedAlcoholic != null) {
            viewBinding.tvSelectedAlcoholic.text = tempSelectedAlcoholic
        } else {
            viewBinding.tvSelectedAlcoholic.text = context.getString(R.string.All_Types)
        }

        presenter.onStart()
    }


    private fun initPresenter() {
        presenter = FilterPresenter(onFilterApplied, CocktailRepository())
        presenter.setView(this)
    }


    private fun setupClickListeners() {
        viewBinding.layoutCategory.setOnClickListener {
            showCategoryDialog()
        }


        viewBinding.layoutAlcoholic.setOnClickListener {
            showAlcoholicDialog()
        }


        viewBinding.btnApply.setOnClickListener {
            onFilterApplied(tempSelectedCategory, tempSelectedAlcoholic)
            dismiss()
        }

        viewBinding.btnClear.setOnClickListener {
            tempSelectedCategory = null
            tempSelectedAlcoholic = null
            viewBinding.tvSelectedCategory.text = context.getString(R.string.All_Categories)
            viewBinding.tvSelectedAlcoholic.text = context.getString(R.string.All_Types)
        }
    }


    private fun showCategoryDialog() {
        if (categories.isNotEmpty()) {
            val categoriesWithAll = listOf(context.getString(R.string.All_Categories)) + categories
            val currentIndex = if (tempSelectedCategory == null) 0 else {
                categories.indexOf(tempSelectedCategory) + 1
            }

            AlertDialog.Builder(context)
                .setTitle("Select Category")
                .setSingleChoiceItems(categoriesWithAll.toTypedArray(), currentIndex) { dialog, which ->
                    tempSelectedCategory = if (which == 0) null else categories[which - 1]
                    viewBinding.tvSelectedCategory.text = categoriesWithAll[which]
                    dialog.dismiss()
                }
                .show()
        }
    }


    private fun showAlcoholicDialog() {
        if (alcoholicTypes.isNotEmpty()) {
            val typesWithAll = listOf(context.getString(R.string.All_Types)) + alcoholicTypes
            val currentIndex = if (tempSelectedAlcoholic == null) 0 else {
                alcoholicTypes.indexOf(tempSelectedAlcoholic) + 1
            }

            AlertDialog.Builder(context)
                .setTitle("Select Type")
                .setSingleChoiceItems(typesWithAll.toTypedArray(), currentIndex) { dialog, which ->
                    tempSelectedAlcoholic = if (which == 0) null else alcoholicTypes[which - 1]
                    viewBinding.tvSelectedAlcoholic.text = typesWithAll[which]
                    dialog.dismiss()
                }
                .show()
        }
    }


    override fun showCategories(categories: List<String>) {
        this.categories = categories
    }


    override fun showAlcoholicTypes(types: List<String>) {
        this.alcoholicTypes = types
    }


    override fun updateSelectedCategory(category: String) {
        viewBinding.tvSelectedCategory.text = category
    }


    override fun updateSelectedAlcoholic(alcoholic: String) {
        viewBinding.tvSelectedAlcoholic.text = alcoholic
    }


    override fun showLoading() {
        // TODO: Show loading dialog or progress bar
    }


    override fun hideLoading() {
        // TODO: Hide loading dialog or progress bar
    }


    override fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    override fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    override fun dismissDialog() {
        dismiss()
    }


    override fun onStop() {
        presenter.onStop()
        super.onStop()
    }
}
