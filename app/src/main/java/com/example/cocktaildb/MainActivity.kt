package com.example.cocktaildb

import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.cocktaildb.databinding.ActivityMainBinding
import com.example.cocktaildb.utils.base.BaseActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.View

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // Ẩn action bar
        supportActionBar?.hide()
        
        // Thiết lập status bar trong suốt
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        // Thiết lập navigation bar trong suốt
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Đảm bảo content không bị đè vào system UI
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        
        val navView: BottomNavigationView = viewBinding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_messages,
                R.id.navigation_add,
                R.id.navigation_favorites,
                R.id.navigation_profile,
            )
        )
        // Xóa setupActionBarWithNavController vì không còn action bar
        navView.setupWithNavController(navController)
    }

    override fun initData() {
        // TODO: Initialize data if needed
    }
}

