package com.example.cocktaildb

import androidx.navigation.findNavController
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.example.cocktaildb.databinding.ActivityMainBinding
import com.example.cocktaildb.utils.base.BaseActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // Hide action bar
        supportActionBar?.hide()

        // Set transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Set transparent navigation bar
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Ensure content doesn't overlap with system UI
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        setupCustomNavigation()
    }

    private fun setupCustomNavigation() {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Set up navigation item click listeners
        viewBinding.navHome.setOnClickListener {
            navController.navigate(R.id.navigation_home)
            updateNavigationSelection(R.id.navigation_home)
        }

        viewBinding.navMessages.setOnClickListener {
            navController.navigate(R.id.navigation_messages)
            updateNavigationSelection(R.id.navigation_messages)
        }

        viewBinding.navAdd.setOnClickListener {
            navController.navigate(R.id.navigation_add)
            updateNavigationSelection(R.id.navigation_add)
        }

        viewBinding.navFavorites.setOnClickListener {
            navController.navigate(R.id.navigation_favorites)
            updateNavigationSelection(R.id.navigation_favorites)
        }

        viewBinding.navProfile.setOnClickListener {
            navController.navigate(R.id.navigation_profile)
            updateNavigationSelection(R.id.navigation_profile)
        }

        // Set initial selection
        updateNavigationSelection(R.id.navigation_home)

        // Ensure Add button always shows plus icon
        viewBinding.navAdd.setColorFilter(ContextCompat.getColor(this, R.color.white))

        // Listen for navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavigationSelection(destination.id)
        }
    }

    private fun updateNavigationSelection(selectedId: Int) {
        // Reset all items to default state
        resetNavigationItems()

        // Highlight selected item
        when (selectedId) {
            R.id.navigation_home -> {
                viewBinding.navHome.setColorFilter(ContextCompat.getColor(this, R.color.pink_primary))
                viewBinding.navHome.background = ContextCompat.getDrawable(this, R.drawable.nav_item_background)
            }
            R.id.navigation_messages -> {
                viewBinding.navMessages.setColorFilter(ContextCompat.getColor(this, R.color.pink_primary))
                viewBinding.navMessages.background = ContextCompat.getDrawable(this, R.drawable.nav_item_background)
            }
            R.id.navigation_add -> {
                // Center button is always highlighted
                viewBinding.navAdd.setColorFilter(ContextCompat.getColor(this, R.color.white))
            }
            R.id.navigation_favorites -> {
                viewBinding.navFavorites.setColorFilter(ContextCompat.getColor(this, R.color.pink_primary))
                viewBinding.navFavorites.background = ContextCompat.getDrawable(this, R.drawable.nav_item_background)
            }
            R.id.navigation_profile -> {
                viewBinding.navProfile.setColorFilter(ContextCompat.getColor(this, R.color.pink_primary))
                viewBinding.navProfile.background = ContextCompat.getDrawable(this, R.drawable.nav_item_background)
            }
        }
    }

    private fun resetNavigationItems() {
        // Reset all items to default state
        val defaultColor = ContextCompat.getColor(this, R.color.gray)

        viewBinding.navHome.setColorFilter(defaultColor)
        viewBinding.navHome.background = null

        viewBinding.navMessages.setColorFilter(defaultColor)
        viewBinding.navMessages.background = null

        viewBinding.navFavorites.setColorFilter(defaultColor)
        viewBinding.navFavorites.background = null

        viewBinding.navProfile.setColorFilter(defaultColor)
        viewBinding.navProfile.background = null

        // Keep Add button always highlighted with white plus icon
        viewBinding.navAdd.setColorFilter(ContextCompat.getColor(this, R.color.white))
    }

    override fun initData() {
        // TODO: Initialize data if needed
    }
}
