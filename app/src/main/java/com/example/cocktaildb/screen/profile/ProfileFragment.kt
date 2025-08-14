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
import com.example.cocktaildb.R
import com.example.cocktaildb.data.model.Cocktail
import com.example.cocktaildb.data.repository.AuthRepository
import com.example.cocktaildb.data.repository.CocktailRepository
import com.example.cocktaildb.data.repository.source.local.CocktailLocalDataSource
import com.example.cocktaildb.databinding.FragmentProfileBinding
import com.example.cocktaildb.databinding.ItemCocktailBinding
import com.example.cocktaildb.databinding.ItemProfileHeaderBinding
import com.example.cocktaildb.screen.auth.SignInActivity
import com.example.cocktaildb.utils.ImageLoader
import com.example.cocktaildb.utils.base.BaseFragment

/**
 * Fragment for displaying user profile and cocktail list
 */
class ProfileFragment : BaseFragment<FragmentProfileBinding>(), ProfileContract.View {

    private lateinit var presenter: ProfileContract.Presenter
    private val profileAdapter = ProfileAdapter()

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
        val authRepository = AuthRepository()
        presenter = ProfilePresenter(repository, authRepository)
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
                "My Recipes feature coming soon!",
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

    /**
     * Adapter for the profile RecyclerView that displays both header and cocktail items
     */
    private inner class ProfileAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_COCKTAIL = 1

        private var userName: String = ""
        private var userBio: String = ""
        private var profileImageUrl: String? = null
        private val cocktails: MutableList<Cocktail> = mutableListOf()

        fun setUserProfile(userName: String, userBio: String, profileImageUrl: String?) {
            this.userName = userName
            this.userBio = userBio
            this.profileImageUrl = profileImageUrl
            notifyItemChanged(0)
        }

        fun setCocktails(cocktails: List<Cocktail>) {
            this.cocktails.clear()
            this.cocktails.addAll(cocktails)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_COCKTAIL
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val binding = ItemProfileHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false)
                    HeaderViewHolder(binding)
                }
                else -> {
                    val binding = ItemCocktailBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false)
                    CocktailViewHolder(binding)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderViewHolder -> {
                    holder.bind(userName, userBio, profileImageUrl)
                }
                is CocktailViewHolder -> {
                    // Position - 1 because header is at position 0
                    holder.bind(cocktails[position - 1])
                }
            }
        }

        override fun getItemCount(): Int = if (cocktails.isEmpty()) 1 else cocktails.size + 1

        /**
         * ViewHolder for the profile header
         */
        inner class HeaderViewHolder(private val binding: ItemProfileHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

            init {
                binding.myRecipeButton.setOnClickListener {
                    presenter.onMyRecipesClicked()
                }

                binding.historyButton.setOnClickListener {
                    presenter.onHistoryClicked()
                }

                binding.logoutButton.setOnClickListener {
                    presenter.onLogoutClicked()
                }
            }

            fun bind(name: String, bio: String, imageUrl: String?) {
                // Ensure we have some text to display
                binding.userName.text = if (name.isNotEmpty()) name else "User"
                binding.userBio.text = if (bio.isNotEmpty()) bio else "Email not available"

                // Use our custom ImageLoader to load the profile image if available
                if (imageUrl != null && imageUrl.isNotEmpty()) {
                    try {
                        ImageLoader.loadImage(
                            url = imageUrl,
                            imageView = binding.profileImageView,
                            placeholderResId = R.drawable.profile_placeholder
                        )
                    } catch (e: Exception) {
                        // If image loading fails, use placeholder
                        binding.profileImageView.setImageResource(R.drawable.profile_placeholder)
                    }
                } else {
                    // Use the profile placeholder if no image URL is provided
                    binding.profileImageView.setImageResource(R.drawable.profile_placeholder)
                }
            }
        }

        /**
         * ViewHolder for cocktail items
         */
        inner class CocktailViewHolder(private val binding: ItemCocktailBinding) : RecyclerView.ViewHolder(binding.root) {

            fun bind(cocktail: Cocktail) {
                binding.tvCocktailName.text = cocktail.strDrink
                binding.tvCocktailCategory.text = cocktail.strCategory ?: "Cocktail"

                // Load the cocktail image using our custom ImageLoader
                try {
                    ImageLoader.loadImage(
                        url = cocktail.strDrinkThumb,
                        imageView = binding.ivCocktail,
                        placeholderResId = R.drawable.ic_launcher_background
                    )
                } catch (e: Exception) {
                    // If image loading fails, use placeholder
                    binding.ivCocktail.setImageResource(R.drawable.ic_launcher_background)
                }

                // Set rating
                binding.tvRating.text = "4.8"
            }
        }
    }
}
