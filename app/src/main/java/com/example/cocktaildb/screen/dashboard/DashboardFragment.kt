package com.example.cocktaildb.screen.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.cocktaildb.databinding.FragmentDashboardBinding
import com.example.cocktaildb.utils.base.BaseFragment

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(), DashboardContract.View {
    
    private lateinit var presenter: DashboardPresenter
    
    override fun inflateViewBinding(inflater: LayoutInflater): FragmentDashboardBinding {
        return FragmentDashboardBinding.inflate(inflater)
    }
    
    override fun initView() {
        // Initialize presenter
        presenter = DashboardPresenter()
        presenter.setView(this)
    }
    
    override fun initData() {
        presenter.onStart()
        presenter.loadDashboardData()
    }
    
    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }
    
    override fun showDashboardData() {
        // TODO: Update UI with dashboard data
        viewBinding.textDashboard.text = "Dashboard loaded successfully"
    }
    
    override fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun showLoading() {
        // TODO: Show loading indicator
    }
    
    override fun hideLoading() {
        // TODO: Hide loading indicator
    }
} 