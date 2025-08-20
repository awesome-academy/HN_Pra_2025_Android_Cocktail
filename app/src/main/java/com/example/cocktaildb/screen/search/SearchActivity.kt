package com.example.cocktaildb.screen.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.databinding.ActivitySearchBinding
import com.example.cocktaildb.screen.detail.CocktailDetailFragment
import com.example.cocktaildb.screen.filter.FilterDialog
import com.example.cocktaildb.utils.adapter.CocktailAdapter
import com.example.cocktaildb.utils.base.BaseActivity
import com.example.cocktaildb.utils.pagination.PaginationUI
import java.util.concurrent.Executors
import android.view.View

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
        viewBinding.searchHeader.visibility = View.GONE
        viewBinding.searchResultsContainer.visibility = View.GONE
    }

    private fun initPresenter() {
        presenter = SearchPresenter()
        presenter.setView(this)
    }

    private fun setupRecyclerView() {
        adapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                showCocktailDetail(cocktail)
            },
            useSearchLayout = true
        )
        viewBinding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        viewBinding.recyclerView.adapter = adapter
    }

    private fun showCocktailDetail(cocktail: Cocktail) {
        Log.d("SearchActivity", "Showing cocktail detail: ${cocktail.strDrink} (${cocktail.idDrink})")

        // Add to history first
        addCocktailToHistory(cocktail)
        
        // Hide search results and show detail container
        viewBinding.searchResultsContainer.visibility = View.GONE
        viewBinding.detailContainer.visibility = View.VISIBLE

        if (intent.getBooleanExtra("from_today_drink", false)) {
            viewBinding.searchHeader.visibility = View.GONE
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

    private fun addCocktailToHistory(cocktail: Cocktail) {
        Log.d("SearchActivity", "addCocktailToHistory called for: ${cocktail.strDrink} (${cocktail.idDrink})")

        val authRepository = AuthRepository(this)
        val currentUser = authRepository.getCurrentUser()

        if (currentUser != null) {
            Log.e("SearchActivity", "User authenticated: ${currentUser.uid}")
            val historyFirebaseService = HistoryFirebaseService()

            lifecycleScope.launch {
                try {
                    Log.e("SearchActivity", "Adding cocktail details to Firebase history: uid=${currentUser.uid}, cocktail=${cocktail.strDrink}")
                    val result = historyFirebaseService.addHistoryWithDetails(currentUser.uid, cocktail)
                    if (result.isSuccess) {
                        Log.e("SearchActivity", "Successfully added detailed history: ${result.getOrNull()}")
                    } else {
                        Log.e("SearchActivity", "Failed to add detailed history: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("SearchActivity", "Exception adding detailed history: ${e.message}", e)
                }
            }
        } else {
            Log.e("SearchActivity", "User not authenticated, skipping history add")
        }
    }

    private fun showSearchResults() {
        // Show search results and hide detail container
        viewBinding.searchResultsContainer.visibility = View.VISIBLE
        viewBinding.detailContainer.visibility = View.GONE

        if (!intent.getBooleanExtra("from_today_drink", false)) {
            viewBinding.searchHeader.visibility = View.VISIBLE
        }
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
