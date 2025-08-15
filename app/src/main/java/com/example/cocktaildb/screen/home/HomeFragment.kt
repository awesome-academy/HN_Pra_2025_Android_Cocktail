package com.example.cocktaildb.screen.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.graphics.Rect
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.remote.CocktailRemoteDataSource
import com.example.cocktaildb.databinding.FragmentHomeBinding
import com.example.cocktaildb.screen.search.SearchActivity
import com.example.cocktaildb.utils.adapter.CocktailAdapter
import com.example.cocktaildb.utils.base.BaseFragment
import kotlin.math.min

class HomeFragment : BaseFragment<FragmentHomeBinding>(), HomeContract.View {

    private lateinit var presenter: HomePresenter
    private lateinit var cocktailAdapter: CocktailAdapter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater)
    }

    override fun initView() {
        // Initialize presenter with remote data source
        val repository = CocktailRepository(CocktailRemoteDataSource())
        presenter = HomePresenter(repository)
        presenter.setView(this)

        // Initialize RecyclerView
        setupRecyclerView()

        // Set up search card click listener
        viewBinding.cardSearch.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        cocktailAdapter = CocktailAdapter(
            items = emptyList(),
            onCocktailClick = { cocktail ->
                Toast.makeText(context, "Selected: ${cocktail.strDrink}", Toast.LENGTH_SHORT).show()
            }
        )

        // Set up RecyclerView with GridLayoutManager showing 2 items per row
        val layoutManager = GridLayoutManager(context, 2)
        
        // Apply proper item spacing decoration
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.dp_8)
        viewBinding.recyclerViewCocktails.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

        viewBinding.recyclerViewCocktails.apply {
            this.layoutManager = layoutManager
            adapter = cocktailAdapter
            clipToPadding = false  // Allow scrolling into the padding area
        }
    }

    override fun initData() {
        presenter.onStart()
        presenter.loadCocktails()
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }

    override fun showCocktails(cocktails: List<Cocktail>) {
        if (cocktails.isEmpty()) {
            Toast.makeText(context, "No cocktails found", Toast.LENGTH_SHORT).show()
            return
        }

        // Skip first 3 cocktails and show from 4th onwards, limit to 8 cocktails
        val filteredCocktails = if (cocktails.size > 3) {
            val startIndex = 3
            val endIndex = min(startIndex + 8, cocktails.size)
            cocktails.subList(startIndex, endIndex)
        } else {
            // If less than 4 cocktails, show all
            cocktails
        }

        if (filteredCocktails.isEmpty()) {
            Toast.makeText(context, "No cocktails available after filtering", Toast.LENGTH_SHORT).show()
            return
        }

        // Update adapter with filtered cocktails
        cocktailAdapter.submit(filteredCocktails)

        // Show success message
        Toast.makeText(context, "Loaded ${filteredCocktails.size} cocktails", Toast.LENGTH_SHORT).show()
    }
}

