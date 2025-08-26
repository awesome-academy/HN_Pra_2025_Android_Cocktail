package com.example.cocktaildb.screen.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.data.service.SearchHistoryService
import com.example.cocktaildb.databinding.ActivitySearchBinding
import com.example.cocktaildb.screen.detail.CocktailDetailFragment
import com.example.cocktaildb.screen.filter.FilterDialog
import com.example.cocktaildb.utils.adapter.CocktailAdapter
import com.example.cocktaildb.utils.adapter.SearchSuggestionAdapter
import com.example.cocktaildb.utils.base.BaseActivity
import com.example.cocktaildb.utils.pagination.PaginationUI
import java.util.concurrent.Executors
import android.view.View
import android.view.inputmethod.EditorInfo

class SearchActivity : BaseActivity<ActivitySearchBinding>(), SearchContract.View {

    private lateinit var presenter: SearchPresenter
    private lateinit var adapter: CocktailAdapter
    private lateinit var suggestionAdapter: SearchSuggestionAdapter
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
        setupSuggestionRecyclerView()
        setupSearchListener()
        setupFilterListener()
        setupPaginationListeners()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (intent.getBooleanExtra("from_today_drink", false)) {
                    finish()
                    return
                }
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    showSearchResults()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun initData() {
        initPresenter()
        presenter.onStart()
        handleIncomingIntent()
    }

    private fun handleIncomingIntent() {
        if (intent.getBooleanExtra("show_detail", false)) {
            if (intent.getBooleanExtra("from_today_drink", false)) {
                hideSearchComponents()
            }
            val cocktail = Cocktail(
                idDrink = intent.getStringExtra("cocktail_id") ?: "",
                strDrink = intent.getStringExtra("cocktail_name") ?: "",
                strCategory = intent.getStringExtra("cocktail_category"),
                strAlcoholic = intent.getStringExtra("cocktail_alcoholic"),
                strGlass = intent.getStringExtra("cocktail_glass"),
                strInstructions = intent.getStringExtra("cocktail_instructions"),
                strDrinkThumb = intent.getStringExtra("cocktail_image"),
                ingredients = intent.getStringArrayExtra("cocktail_ingredients")?.toList() ?: emptyList(),
                measures = intent.getStringArrayExtra("cocktail_measures")?.toList() ?: emptyList()
            )
            showCocktailDetail(cocktail)
        }
    }

    private fun hideSearchComponents() {
        viewBinding.searchHeaderContainer.visibility = View.GONE
        viewBinding.searchResultsContainer.visibility = View.GONE
    }

    private fun initPresenter() {
        // Initialize presenter with dependency injection
        val cocktailRepository = com.example.cocktaildb.data.repository.CocktailRepository(
            com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource()
        )
        val authRepository = AuthRepository(this)
        val historyFirebaseService = HistoryFirebaseService()
        val searchHistoryService = SearchHistoryService(this)

        presenter = SearchPresenter(cocktailRepository, authRepository, historyFirebaseService, searchHistoryService)
        presenter.setView(this)
    }

    private fun setupRecyclerView() {
        adapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                presenter.onCocktailClicked(cocktail)
            },
            useSearchLayout = true
        )
        viewBinding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        viewBinding.recyclerView.adapter = adapter
    }

    private fun setupSuggestionRecyclerView() {
        suggestionAdapter = SearchSuggestionAdapter(
            onSuggestionClick = { suggestion ->
                presenter.onSuggestionClicked(suggestion)
            },
            onSuggestionRemove = { suggestion ->
                presenter.onSuggestionRemoved(suggestion)
            }
        )
        viewBinding.recyclerSearchSuggestions.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerSearchSuggestions.adapter = suggestionAdapter
    }

    override fun navigateToCocktailDetail(cocktail: Cocktail) {
        showCocktailDetail(cocktail)
    }

    private fun showCocktailDetail(cocktail: Cocktail) {
        Log.d("SearchActivity", "Showing cocktail detail: ${cocktail.strDrink} (${cocktail.idDrink})")

        // Hide search results and show detail container
        viewBinding.searchResultsContainer.visibility = View.GONE
        viewBinding.detailContainer.visibility = View.VISIBLE

        if (intent.getBooleanExtra("from_today_drink", false)) {
            viewBinding.searchHeaderContainer.visibility = View.GONE
        }

        // Create and show detail fragment
        val fragment = CocktailDetailFragment().apply {
            arguments = Bundle().apply {
                putString(CocktailDetailFragment.KEY_COCKTAIL_ID, cocktail.idDrink)
                putString(CocktailDetailFragment.KEY_COCKTAIL_NAME, cocktail.strDrink)
                putString(CocktailDetailFragment.KEY_COCKTAIL_CATEGORY, cocktail.strCategory ?: "")
                putString(CocktailDetailFragment.KEY_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic ?: "")
                putString(CocktailDetailFragment.KEY_COCKTAIL_GLASS, cocktail.strGlass ?: "")
                putString(CocktailDetailFragment.KEY_COCKTAIL_INSTRUCTIONS, cocktail.strInstructions ?: "")
                putString(CocktailDetailFragment.KEY_COCKTAIL_IMAGE, cocktail.strDrinkThumb ?: "")
                putStringArray(CocktailDetailFragment.KEY_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
                putStringArray(CocktailDetailFragment.KEY_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
            }
        }

        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.detailContainer, fragment)
        if (!intent.getBooleanExtra("from_today_drink", false)) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    private fun showSearchResults() {
        // Show search results and hide detail container
        viewBinding.searchResultsContainer.visibility = View.VISIBLE
        viewBinding.detailContainer.visibility = View.GONE

        if (!intent.getBooleanExtra("from_today_drink", false)) {
            viewBinding.searchHeaderContainer.visibility = View.VISIBLE
        }
    }

    private fun setupSearchListener() {
        // Text change listener for suggestions
        viewBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                presenter.onSearchTextChanged(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        viewBinding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                presenter.onSearchFocused()
            } else {
                hideSearchSuggestions()
            }
        }
        viewBinding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = viewBinding.etSearch.text?.toString() ?: ""
                presenter.onSearchSubmitted(query)
                viewBinding.etSearch.clearFocus()
                true
            } else {
                false
            }
        }
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
    override fun showSearchSuggestions(suggestions: List<String>) {
        if (suggestions.isNotEmpty()) {
            suggestionAdapter.updateSuggestions(suggestions)
            viewBinding.recyclerSearchSuggestions.visibility = View.VISIBLE
        } else {
            hideSearchSuggestions()
        }
    }

    override fun hideSearchSuggestions() {
        viewBinding.recyclerSearchSuggestions.visibility = View.GONE
        suggestionAdapter.updateSuggestions(emptyList())
    }

    override fun updateSearchText(text: String) {
        viewBinding.etSearch.setText(text)
        viewBinding.etSearch.setSelection(text.length)
    }

    override fun showLoading() {
        viewBinding.layoutLoading.visibility = View.VISIBLE
        viewBinding.recyclerView.visibility = View.GONE
        viewBinding.layoutEmpty.visibility = View.GONE
        viewBinding.layoutPagination.visibility = View.GONE
    }

    override fun hideLoading() {
        viewBinding.layoutLoading.visibility = View.GONE
    }

    override fun showCocktails(cocktails: List<Cocktail>) {
        hideLoading()
        hideEmptyState()
        
        if (cocktails.isEmpty()) {
            showEmptyState("Không tìm thấy kết quả theo yêu cầu của bạn")
        } else {
            viewBinding.recyclerView.visibility = View.VISIBLE
            adapter.submit(cocktails)
        }
    }

    override fun showEmptyState(message: String) {
        viewBinding.layoutEmpty.visibility = View.VISIBLE
        viewBinding.recyclerView.visibility = View.GONE
        viewBinding.layoutPagination.visibility = View.GONE
        viewBinding.tvEmptyMessage.text = message
    }

    override fun hideEmptyState() {
        viewBinding.layoutEmpty.visibility = View.GONE
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
