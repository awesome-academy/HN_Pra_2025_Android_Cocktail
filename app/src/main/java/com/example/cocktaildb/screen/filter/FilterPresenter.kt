package com.example.cocktaildb.screen.filter

import android.os.Handler
import android.os.Looper
import com.example.cocktaildb.data.repository.CocktailRepository
import java.util.concurrent.Executors
import kotlin.collections.get

class FilterPresenter(
    private val onFilterApplied: (category: String?, alcoholic: String?) -> Unit
) : FilterContract.Presenter {

    private var view: FilterContract.View? = null
    private val cocktailRepository = CocktailRepository()
    private val executor = Executors.newSingleThreadExecutor()

    private var selectedCategory: String? = null
    private var selectedAlcoholic: String? = null
    private val alcoholicTypes = listOf("Alcoholic", "Non alcoholic", "Optional alcohol")

    override fun setView(view: FilterContract.View?) {
        this.view = view
    }

    override fun onStart() {
        loadFilterOptions()
    }

    override fun onStop() {
        view = null
    }

    override fun loadFilterOptions() {
        view?.showLoading()
        executor.execute {
            try {
                val categories = cocktailRepository.getCategories()

                view?.let { v ->
                    Handler(Looper.getMainLooper()).post {
                        v.hideLoading()
                        v.showCategories(categories)
                        v.showAlcoholicTypes(alcoholicTypes)
                    }
                }
            } catch (e: Exception) {
                view?.let { v ->
                    Handler(Looper.getMainLooper()).post {
                        v.hideLoading()
                        v.showError("Error loading filter options: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onCategorySelected(category: String) {
        selectedCategory = category
        view?.updateSelectedCategory(category)
    }

    override fun onAlcoholicSelected(alcoholic: String) {
        selectedAlcoholic = alcoholic
        view?.updateSelectedAlcoholic(alcoholic)
    }

    override fun onFilterApplied() {
        onFilterApplied.invoke(selectedCategory, selectedAlcoholic)
        view?.dismissDialog()
    }
}

