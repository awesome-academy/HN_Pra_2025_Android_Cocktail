package com.example.cocktaildb.screen.allcocktails

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.databinding.FragmentAllCocktailsBinding
import com.example.cocktaildb.screen.detail.CocktailDetailFragment
import com.example.cocktaildb.screen.filter.FilterDialog
import com.example.cocktaildb.utils.adapter.CocktailAdapter
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.pagination.PaginationUI

class AllCocktailsFragment : BaseFragment<FragmentAllCocktailsBinding>(), AllCocktailsContract.View {

    private lateinit var presenter: AllCocktailsPresenter
    private lateinit var adapter: CocktailAdapter
    private lateinit var paginationUI: PaginationUI
    
    private var currentFilterCategory: String? = null
    private var currentFilterAlcoholic: String? = null

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentAllCocktailsBinding {
        return FragmentAllCocktailsBinding.inflate(inflater)
    }

    override fun initView() {
        android.util.Log.d("AllCocktailsFragment", "initView called")
        try {
            paginationUI = PaginationUI(requireContext())
            android.util.Log.d("AllCocktailsFragment", "PaginationUI initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("AllCocktailsFragment", "Error initializing PaginationUI", e)
        }
        setupRecyclerView()
        setupSearchListener()
        setupFilterListener()
        setupPaginationListeners()
    }

    override fun initData() {
        android.util.Log.d("AllCocktailsFragment", "initData called")
        initPresenter()
        android.util.Log.d("AllCocktailsFragment", "Presenter initialized, calling onStart")
        presenter.onStart()
        android.util.Log.d("AllCocktailsFragment", "onStart called, now loading cocktails")
        presenter.loadAllCocktails()
    }

    private fun initPresenter() {
        val cocktailRepository = CocktailRepository(CocktailRemoteDataSource())
        val authRepository = AuthRepository(requireContext())
        val historyFirebaseService = HistoryFirebaseService()

        presenter = AllCocktailsPresenter(cocktailRepository, authRepository, historyFirebaseService)
        presenter.setView(this)
    }

    private fun setupRecyclerView() {
        android.util.Log.d("AllCocktailsFragment", "setupRecyclerView called")
        adapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                android.util.Log.d("AllCocktailsFragment", "Cocktail clicked: ${cocktail.strDrink}")
                presenter.onCocktailClicked(cocktail)
            },
            useSearchLayout = true
        )
        
        val layoutManager = GridLayoutManager(context, 2)
        viewBinding.recyclerView.layoutManager = layoutManager
        viewBinding.recyclerView.adapter = adapter
        android.util.Log.d("AllCocktailsFragment", "RecyclerView setup completed")
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
            context = requireContext(),
            onFilterApplied = { category, alcoholicType ->
                currentFilterCategory = category
                currentFilterAlcoholic = alcoholicType
                applyFilter(category, alcoholicType)
            },
            selectedCategory = currentFilterCategory,
            selectedAlcoholic = currentFilterAlcoholic
        )
        filterDialog.show()
    }

    private fun applyFilter(category: String?, alcoholicType: String?) {
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
                presenter.loadAllCocktails()
            }
        }
    }

    override fun showCocktails(cocktails: List<Cocktail>) {
        android.util.Log.d("AllCocktailsFragment", "showCocktails called with ${cocktails.size} cocktails")
        try {
            adapter.submit(cocktails)
            android.util.Log.d("AllCocktailsFragment", "Adapter updated successfully")
        } catch (e: Exception) {
            android.util.Log.e("AllCocktailsFragment", "Error updating adapter", e)
        }
    }

    override fun showLoadingState() {
        showLoading()
    }

    override fun hideLoadingState() {
        hideLoading()
    }

    override fun showErrorMessage(message: String) {
        showError(message)
    }

    override fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun navigateToCocktailDetail(cocktail: Cocktail) {
        showCocktailDetail(cocktail)
    }

    override fun showCocktailDetail(cocktail: Cocktail) {
        Log.d("AllCocktailsFragment", "Showing cocktail detail: ${cocktail.strDrink} (${cocktail.idDrink})")

        // Hide search results and show detail container
        viewBinding.searchResultsContainer.visibility = View.GONE
        viewBinding.detailContainer.visibility = View.VISIBLE

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

        val transaction = childFragmentManager.beginTransaction()
            .replace(R.id.detailContainer, fragment)
            .addToBackStack(null)
        transaction.commit()
    }

    override fun showSearchResults() {
        // Show search results and hide detail container
        viewBinding.searchResultsContainer.visibility = View.VISIBLE
        viewBinding.detailContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }
} 

