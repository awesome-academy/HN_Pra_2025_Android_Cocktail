package com.example.cocktaildb.screen.checkmark

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.databinding.FragmentCheckmarkBinding
import com.example.cocktaildb.utils.base.BaseFragment

class CheckmarkFragment : BaseFragment<FragmentCheckmarkBinding>(), CheckmarkContract.View {

    private lateinit var presenter: CheckmarkPresenter
    private lateinit var checkmarkAdapter: CheckmarkAdapter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentCheckmarkBinding {
        return FragmentCheckmarkBinding.inflate(inflater)
    }

    override fun initView() {
        presenter = CheckmarkPresenter(requireContext(), CocktailRepository())
        setupRecyclerView()
    }

    override fun initData() {
        presenter.setView(this)
        if (!hasDataLoaded()) {
            presenter.loadCheckmarks()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasDataLoaded()) {
            presenter.loadCheckmarks()
        }
    }

    private fun hasDataLoaded(): Boolean {
        return ::checkmarkAdapter.isInitialized && checkmarkAdapter.itemCount > 0
    }

    private fun setupRecyclerView() {
        // Initialize adapter with presenter and NavController
        checkmarkAdapter = CheckmarkAdapter(presenter, findNavController())

        viewBinding.checkmarkRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = checkmarkAdapter

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

    // Implement CheckmarkContract.View methods with names that don't conflict with BaseFragment
    override fun displayLoading(show: Boolean) {
        viewBinding.loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        viewBinding.checkmarkRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        viewBinding.emptyStateView.visibility = View.GONE
    }

    override fun displayCheckmarks(checkmarks: List<Cocktail>) {
        viewBinding.loadingProgressBar.visibility = View.GONE
        viewBinding.emptyStateView.visibility = if (checkmarks.isEmpty()) View.VISIBLE else View.GONE
        viewBinding.checkmarkRecyclerView.visibility = if (checkmarks.isEmpty()) View.GONE else View.VISIBLE
        checkmarkAdapter.submitList(checkmarks)
    }

    override fun displayEmptyState() {
        viewBinding.loadingProgressBar.visibility = View.GONE
        viewBinding.checkmarkRecyclerView.visibility = View.GONE
        viewBinding.emptyStateView.visibility = View.VISIBLE
    }

    override fun displayError(message: String) {
        viewBinding.loadingProgressBar.visibility = View.GONE
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showCheckmarkAdded(cocktail: Cocktail) {
        Toast.makeText(context, "Added ${cocktail.strDrink} to checkmarks", Toast.LENGTH_SHORT).show()
    }

    override fun showCheckmarkRemoved(cocktail: Cocktail) {
        Toast.makeText(context, "Removed ${cocktail.strDrink} from checkmarks", Toast.LENGTH_SHORT).show()
    }

    override fun showSyncStatus(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
} 