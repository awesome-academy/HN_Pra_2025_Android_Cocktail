package com.example.cocktaildb.screen.favorites

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.databinding.FragmentFavoritesBinding
import com.example.cocktaildb.utils.base.BaseFragment

class FavoritesFragment : BaseFragment<FragmentFavoritesBinding>(), FavoritesContract.View {

    private val presenter = FavoritesPresenter()
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentFavoritesBinding {
        return FragmentFavoritesBinding.inflate(inflater)
    }

    override fun initView() {
        setupRecyclerView()
    }

    override fun initData() {
        presenter.setView(this)
        presenter.onStart()
    }

    override fun onResume() {
        super.onResume()
        // Ensure presenter has a view reference before loading
        presenter.setView(this)
        // Force reload favorites
        presenter.loadFavorites()
    }

    override fun onPause() {
        super.onPause()
        // Clear view reference to avoid memory leaks
        presenter.setView(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onStop()
    }

    private fun setupRecyclerView() {
        // Initialize adapter with presenter and NavController
        favoritesAdapter = FavoritesAdapter(presenter, findNavController())

        viewBinding.favoritesRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = favoritesAdapter

            // Add spacing between grid items using ItemDecoration
            val spacingInPixels = resources.getDimensionPixelSize(R.dimen.dp_8)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)

                    // Determine if this is an item in the left or right column
                    val isLeftColumn = position % 2 == 0

                    // Add spacing to all items
                    outRect.bottom = spacingInPixels

                    // Add more space on the left for left column items and on the right for right column items
                    if (isLeftColumn) {
                        outRect.left = spacingInPixels * 2
                        outRect.right = spacingInPixels
                    } else {
                        outRect.left = spacingInPixels
                        outRect.right = spacingInPixels * 2
                    }

                    // Add top margin for the first row items
                    if (position == 0 || position == 1) {
                        outRect.top = spacingInPixels
                    }
                }
            })
        }
    }

    // Implement FavoritesContract.View methods with names that don't conflict with BaseFragment
    override fun displayLoading(show: Boolean) {
        if (show) {
            super.showLoading()
            viewBinding.favoritesRecyclerView.visibility = View.GONE
            viewBinding.emptyStateView.visibility = View.GONE
        } else {
            super.hideLoading()
        }
    }

    override fun displayFavorites(favorites: List<Cocktail>) {
        viewBinding.favoritesRecyclerView.visibility = View.VISIBLE
        viewBinding.emptyStateView.visibility = View.GONE
        favoritesAdapter.submitList(favorites)
    }

    override fun displayEmptyState() {
        viewBinding.favoritesRecyclerView.visibility = View.GONE
        viewBinding.emptyStateView.visibility = View.VISIBLE
    }

    override fun displayError(message: String) {
        super.showError(message)
    }

    override fun showFavoriteAdded(cocktail: Cocktail) {
        Toast.makeText(context, "${cocktail.strDrink} added to favorites", Toast.LENGTH_SHORT).show()
    }

    override fun showFavoriteRemoved(cocktail: Cocktail) {
        Toast.makeText(context, "${cocktail.strDrink} removed from favorites", Toast.LENGTH_SHORT).show()
    }
}
