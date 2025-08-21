package com.example.cocktaildb.screen.history

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.databinding.FragmentHistoryBinding
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.utils.adapter.CocktailAdapter
import com.example.cocktaildb.screen.history.HistoryPresenter

class HistoryFragment : BaseFragment<FragmentHistoryBinding>(), HistoryContract.View {

    private lateinit var presenter: HistoryContract.Presenter
    private lateinit var historyAdapter: CocktailAdapter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentHistoryBinding {
        return FragmentHistoryBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apply window insets to handle navigation bar correctly
        ViewCompat.setOnApplyWindowInsetsListener(requireView()) { _, insets ->
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.dp_8)

            // Apply bottom margin to account for both navigation bar and bottom nav
            requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.historyRecyclerView).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navigationBarInsets.bottom + bottomNavHeight
            }

            insets
        }
    }

    override fun initView() {
        setupToolbar()
        setupRecyclerView()
        setupLoadingView()
    }

    private fun setupToolbar() {
        requireView().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set up menu
        requireView().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_history -> {
                    showClearHistoryDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                presenter.onCocktailClicked(cocktail)
            }
        )

        // Set up RecyclerView with GridLayoutManager showing 2 items per row
        val layoutManager = GridLayoutManager(context, 2)
        
        // Apply proper item spacing decoration
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.dp_8)
        requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.historyRecyclerView).addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                // Apply spacing to all items
                outRect.left = spacingInPixels
                outRect.right = spacingInPixels
                outRect.bottom = spacingInPixels

                // Determine if this is an item in the left or right column
                val isLeftColumn = position % 2 == 0

                // Add more space on the left for left column items and on the right for right column items
                if (isLeftColumn) {
                    outRect.left = spacingInPixels * 2
                } else {
                    outRect.right = spacingInPixels * 2
                }

                // Add top margin only for the first row items
                if (position == 0 || position == 1) {
                    outRect.top = spacingInPixels
                }
            }
        })

        requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.historyRecyclerView).apply {
            this.layoutManager = layoutManager
            adapter = historyAdapter
            clipToPadding = false
            setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_nav_height))
        }
    }

    private fun setupLoadingView() {
        requireView().findViewById<android.view.View>(R.id.loadingView).visibility = android.view.View.GONE
    }

    override fun initData() {
        val dataSource = CocktailLocalDataSource()
        val repository = CocktailRepository(dataSource)
        val contextWrapper = com.example.cocktaildb.utils.CocktailContextWrapper(requireContext(), this)
        presenter = HistoryPresenter(repository, contextWrapper)
        presenter.setView(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.onStart()
    }

    override fun onPause() {
        presenter.onStop()
        super.onPause()
    }

    override fun showHistoryCocktails(cocktails: List<Cocktail>) {
        historyAdapter.submit(cocktails)
        requireView().findViewById<android.view.View>(R.id.historyRecyclerView).visibility = android.view.View.VISIBLE
        requireView().findViewById<android.view.View>(R.id.emptyStateLayout).visibility = android.view.View.GONE
    }

    override fun showEmptyState() {
        requireView().findViewById<android.view.View>(R.id.historyRecyclerView).visibility = android.view.View.GONE
        requireView().findViewById<android.view.View>(R.id.emptyStateLayout).visibility = android.view.View.VISIBLE
    }

    override fun hideEmptyState() {
        requireView().findViewById<android.view.View>(R.id.emptyStateLayout).visibility = android.view.View.GONE
    }

    override fun displayLoading(show: Boolean) {
        requireView().findViewById<android.view.View>(R.id.loadingView).visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun displayError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToCocktailDetail(cocktail: Cocktail) {
        // Navigate to cocktail detail fragment using Navigation Component
        val bundle = Bundle().apply {
            putString("cocktail_id", cocktail.idDrink)
            putString("cocktail_name", cocktail.strDrink)
            putString("cocktail_category", cocktail.strCategory ?: "")
            putString("cocktail_alcoholic", cocktail.strAlcoholic ?: "")
            putString("cocktail_glass", cocktail.strGlass ?: "")
            putString("cocktail_instructions", cocktail.strInstructions ?: "")
            putString("cocktail_image", cocktail.strDrinkThumb ?: "")
            putStringArray("cocktail_ingredients", cocktail.ingredients.toTypedArray())
            putStringArray("cocktail_measures", cocktail.measures.toTypedArray())
        }
        findNavController().navigate(R.id.navigation_cocktail_detail, bundle)
    }

    private fun showClearHistoryDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all history? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                presenter.clearHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
