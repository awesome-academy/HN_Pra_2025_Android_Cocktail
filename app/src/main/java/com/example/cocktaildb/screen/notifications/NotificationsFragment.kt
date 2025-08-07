package com.example.cocktaildb.screen.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.cocktaildb.databinding.FragmentNotificationsBinding
import com.example.cocktaildb.utils.base.BaseFragment

class NotificationsFragment : BaseFragment<FragmentNotificationsBinding>(), NotificationsContract.View {

    private lateinit var presenter: NotificationsPresenter

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentNotificationsBinding {
        return FragmentNotificationsBinding.inflate(inflater)
    }

    override fun initView() {
        // Initialize presenter
        presenter = NotificationsPresenter()
        presenter.setView(this)
    }

    override fun initData() {
        presenter.onStart()
        presenter.loadNotifications()
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }

    override fun showNotifications() {
        // TODO: Update UI with notifications
        viewBinding.textNotifications.text = "Notifications loaded successfully"
    }
}

