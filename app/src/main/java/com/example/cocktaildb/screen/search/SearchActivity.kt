package com.example.cocktaildb.screen.search

import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.ActivitySearchBinding
import com.example.cocktaildb.screen.filter.FilterDialog
import com.example.cocktaildb.utils.adapter.CocktailAdapter
import com.example.cocktaildb.utils.base.BaseActivity
import com.example.cocktaildb.utils.pagination.PaginationUI
import java.util.concurrent.Executors

class SearchActivity : BaseActivity<ActivitySearchBinding>(), SearchContract.View {

    private lateinit var presenter: SearchPresenter
    private lateinit var adapter: CocktailAdapter
    private lateinit var paginationUI: PaginationUI
    private val executor = Executors.newSingleThreadExecutor()
    private var currentFilterCategory: String? = null
    private var currentFilterAlcoholic: String? = null

    override fun inflateViewBinding(): ActivitySearchBinding {
        return ActivitySearchBinding.inflate(layoutInflater)
    }

    override fun initView() {
        supportActionBar?.hide()
        paginationUI = PaginationUI(this)
        setupRecyclerView()
        setupSearchListener()
        setupFilterListener()
        setupPaginationListeners()
    }

    override fun initData() {
        initPresenter()
        presenter.onStart()
    }

    private fun initPresenter() {
        presenter = SearchPresenter()
        presenter.setView(this)
    }

    private fun setupRecyclerView() {
        adapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                Toast.makeText(this, "Selected: ${cocktail.strDrink}", Toast.LENGTH_SHORT).show()
            },
            useSearchLayout = true
        )
        viewBinding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        viewBinding.recyclerView.adapter = adapter
    }

    private fun setupSearchListener() {
        viewBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                presenter.searchCocktails(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFilterListener() {
        viewBinding.btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupPaginationListeners() {
        viewBinding.btnNext.setOnClickListener {
            presenter.nextPage()
        }
        
        viewBinding.btnPrevious.setOnClickListener {
            presenter.previousPage()
        }
    }

    private fun showFilterDialog() {
        val filterDialog = FilterDialog(
            context = this,
            selectedCategory = currentFilterCategory,
            selectedAlcoholic = currentFilterAlcoholic,
            onFilterApplied = { category, alcoholicType ->
                currentFilterCategory = category
                currentFilterAlcoholic = alcoholicType
                applyFilter(category, alcoholicType)
            }
        )
        filterDialog.show()
    }

    private fun applyFilter(category: String?, alcoholicType: String?) {
        val currentQuery = viewBinding.etSearch.text?.toString() ?: ""
        
        when {
            category != null && alcoholicType != null -> {
                presenter.filterByCategory(category)
            }
            category != null -> {
                presenter.filterByCategory(category)
            }
            alcoholicType != null -> {
                presenter.filterByAlcoholic(alcoholicType)
            }
            else -> {
                presenter.searchCocktails(currentQuery)
            }
        }
    }

    override fun showLoading() {
        // TODO: Show loading dialog or progress bar
    }

    override fun hideLoading() {
        // TODO: Hide loading dialog or progress bar
    }

    override fun showCocktails(cocktails: List<Cocktail>) {
        adapter.submit(cocktails)
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun updatePagination(currentPage: Int, totalPages: Int, hasNext: Boolean, hasPrevious: Boolean) {
        viewBinding.btnNext.isEnabled = hasNext
        viewBinding.btnNext.alpha = if (hasNext) 1.0f else 0.5f
        
        viewBinding.btnPrevious.isEnabled = hasPrevious
        viewBinding.btnPrevious.alpha = if (hasPrevious) 1.0f else 0.5f

        paginationUI.createPageButtons(
            viewBinding.layoutPageNumbers,
            currentPage,
            totalPages
        ) { page ->
            presenter.goToPage(page)
        }
    }

    override fun showPagination(show: Boolean) {
        viewBinding.layoutPagination.visibility = if (show) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    override fun onStop() {
        presenter.onStop()
        super.onStop()
    }
}

