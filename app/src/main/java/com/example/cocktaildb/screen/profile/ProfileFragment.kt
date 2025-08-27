package com.example.cocktaildb.screen.profile

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.GlobalScope
import androidx.navigation.fragment.findNavController
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.data.service.HistoryFirebaseService
import com.example.cocktaildb.databinding.FragmentProfileBinding
import com.example.cocktaildb.screen.auth.SignInActivity
import com.example.cocktaildb.utils.base.BaseFragment
import kotlinx.coroutines.launch


class ProfileFragment : BaseFragment<FragmentProfileBinding>(), ProfileContract.View, ProfileAdapter.HeaderClickListener, ProfileAdapter.CocktailClickListener {

    companion object {
        private const val EXTRA_COCKTAIL_ID = "cocktail_id"
        private const val EXTRA_COCKTAIL_NAME = "cocktail_name"
        private const val EXTRA_COCKTAIL_CATEGORY = "cocktail_category"
        private const val EXTRA_COCKTAIL_ALCOHOLIC = "cocktail_alcoholic"
        private const val EXTRA_COCKTAIL_GLASS = "cocktail_glass"
        private const val EXTRA_COCKTAIL_INSTRUCTIONS = "cocktail_instructions"
        private const val EXTRA_COCKTAIL_IMAGE = "cocktail_image"
        private const val EXTRA_COCKTAIL_INGREDIENTS = "cocktail_ingredients"
        private const val EXTRA_COCKTAIL_MEASURES = "cocktail_measures"
    }

    private lateinit var presenter: ProfileContract.Presenter
    private lateinit var profileAdapter: ProfileAdapter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply window insets to handle navigation bar correctly
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { _, insets ->
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height)

            // Apply bottom margin to account for both navigation bar and bottom nav
            viewBinding.profileRecyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navigationBarInsets.bottom + bottomNavHeight
            }

            insets
        }
    }

    override fun initView() {
        // Initialize the adapter with this fragment as both click listeners
        profileAdapter = ProfileAdapter(this, this)

        // Set up RecyclerView with GridLayoutManager showing 2 items per row
        val layoutManager = GridLayoutManager(context, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0) 2 else 1 // Header takes full width, cocktails take half
            }
        }

        // Apply proper item spacing decoration
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.dp_8)
        viewBinding.profileRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position > 0) { // Skip header
                    // Apply spacing to all items except header
                    outRect.left = spacingInPixels
                    outRect.right = spacingInPixels
                    outRect.bottom = spacingInPixels

                    // Determine if this is an item in the left or right column
                    val isLeftColumn = (position - 1) % 2 == 0

                    // Add more space on the left for left column items and on the right for right column items
                    if (isLeftColumn) {
                        outRect.left = spacingInPixels * 2
                    } else {
                        outRect.right = spacingInPixels * 2
                    }

                    // Add top margin only for the first row items
                    if (position == 1 || position == 2) {
                        outRect.top = spacingInPixels
                    }
                }
            }
        })

        viewBinding.profileRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = profileAdapter
            clipToPadding = false  // Allow scrolling into the padding area
            setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.bottom_nav_height))
        }

        // Set up loading view
        viewBinding.loadingView.visibility = View.GONE
    }

    override fun initData() {
        // Initialize presenter with repositories
        val dataSource = CocktailLocalDataSource()
        val repository = CocktailRepository(dataSource)
        val authRepository = AuthRepository(requireContext())
        presenter = ProfilePresenter(requireContext(), repository, authRepository)
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

    override fun showUserProfile(userName: String, userBio: String, profileImageUrl: String?) {
        profileAdapter.setUserProfile(userName, userBio, profileImageUrl)
    }

    override fun showUserCocktails(cocktails: List<Cocktail>) {
        profileAdapter.setCocktails(cocktails)
    }

    override fun navigateToMyRecipes() {
        try {
            // Navigate to the MyRecipe screen using the Navigation Component
            val navController = androidx.navigation.Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment_activity_main
            )
            navController.navigate(R.id.navigation_my_recipe)
        } catch (e: Exception) {
            // Fallback in case navigation fails
            Toast.makeText(
                context,
                getString(R.string.msg_my_recipes_coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun navigateToHistory() {
        try {
            // Navigate to the History screen using the Navigation Component
            val navController = androidx.navigation.Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment_activity_main
            )
            navController.navigate(R.id.navigation_history)
        } catch (e: Exception) {
            // Fallback in case navigation fails
            Toast.makeText(
                context,
                getString(R.string.msg_history_coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun navigateToCheckmarks() {
        try {
            // Navigate to the Checkmark screen using the Navigation Component
            val navController = androidx.navigation.Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment_activity_main
            )
            navController.navigate(R.id.navigation_checkmark)
        } catch (e: Exception) {
            // Fallback in case navigation fails
            Toast.makeText(
                context,
                getString(R.string.msg_checkmarks_coming_soon),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun navigateToLogin() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun displayLoading(show: Boolean) {
        viewBinding.loadingView.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun displayError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Implement HeaderClickListener interface methods
    override fun onMyRecipesClicked() {
        presenter.onMyRecipesClicked()
    }

    override fun onCheckmarkClicked() {
        presenter.onCheckmarkClicked()
    }

    override fun onHistoryClicked() {
        presenter.onHistoryClicked()
    }

    override fun onLogoutClicked() {
        presenter.onLogoutClicked()
    }

    // Implement CocktailClickListener interface method
    override fun onCocktailClicked(cocktail: Cocktail) {
        // Navigate to cocktail detail fragment using Navigation Component
        val bundle = Bundle().apply {
            putString(EXTRA_COCKTAIL_ID, cocktail.idDrink)
            putString(EXTRA_COCKTAIL_NAME, cocktail.strDrink)
            putString(EXTRA_COCKTAIL_CATEGORY, cocktail.strCategory ?: "")
            putString(EXTRA_COCKTAIL_ALCOHOLIC, cocktail.strAlcoholic ?: "")
            putString(EXTRA_COCKTAIL_GLASS, cocktail.strGlass ?: "")
            putString(EXTRA_COCKTAIL_INSTRUCTIONS, cocktail.strInstructions ?: "")
            putString(EXTRA_COCKTAIL_IMAGE, cocktail.strDrinkThumb ?: "")
            putStringArray(EXTRA_COCKTAIL_INGREDIENTS, cocktail.ingredients.toTypedArray())
            putStringArray(EXTRA_COCKTAIL_MEASURES, cocktail.measures.toTypedArray())
        }
        findNavController().navigate(R.id.navigation_cocktail_detail, bundle)
    }
}
