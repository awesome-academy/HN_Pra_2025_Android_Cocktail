package com.example.cocktaildb.screen.filter

interface FilterContract {

    interface View {
        fun showCategories(categories: List<String>)
        fun showAlcoholicTypes(types: List<String>)
        fun updateSelectedCategory(category: String)
        fun updateSelectedAlcoholic(alcoholic: String)
        fun showLoading()
        fun hideLoading()
        fun showError(message: String)
        fun showMessage(message: String)
        fun dismissDialog()
    }

    interface Presenter {
        fun setView(view: View?)
        fun onStart()
        fun onStop()
        fun loadFilterOptions()
        fun onCategorySelected(category: String)
        fun onAlcoholicSelected(alcoholic: String)
        fun onFilterApplied()
    }
}

