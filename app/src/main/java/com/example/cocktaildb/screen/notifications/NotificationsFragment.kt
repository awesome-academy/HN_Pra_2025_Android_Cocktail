package com.example.cocktaildb.screen.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.cocktaildb.databinding.FragmentNotificationsBinding
import com.example.cocktaildb.service.NotificationService
import com.example.cocktaildb.utils.NotificationManager
import com.example.cocktaildb.utils.base.BaseFragment
import com.example.cocktaildb.R

class NotificationsFragment : BaseFragment<FragmentNotificationsBinding>(), NotificationsContract.View {

    private lateinit var presenter: NotificationsPresenter
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationService: NotificationService

    // Activity result launcher for notification permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showImmediateNotification()
            scheduleDailyNotification()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun inflateViewBinding(inflater: LayoutInflater): FragmentNotificationsBinding {
        return FragmentNotificationsBinding.inflate(inflater)
    }

    override fun initView() {
        // Initialize presenter
        presenter = NotificationsPresenter()
        presenter.setView(this)
        
        // Initialize notification components
        notificationManager = NotificationManager(requireContext())
        notificationService = NotificationService()
        
        // Set up click listeners
        setupClickListeners()
    }

    override fun initData() {
        presenter.onStart()
        presenter.loadNotifications()
        
        // Check and request notification permission
        checkNotificationPermission()
        
        // Hiển thị thông báo test ngay khi vào màn hình
        showTestNotificationOnLoad()
    }

    override fun onDestroyView() {
        presenter.onStop()
        super.onDestroyView()
    }

    override fun showNotifications() {
        // Update UI with notification controls and show random notification message
        val randomMessage = getRandomNotificationMessage()
        viewBinding.textNotifications.text = randomMessage
    }
    
    private fun getRandomNotificationMessage(): String {
        val messages = listOf(
            requireContext().getString(R.string.notification_message_1),
            requireContext().getString(R.string.notification_message_2),
            requireContext().getString(R.string.notification_message_3),
            requireContext().getString(R.string.notification_message_4),
            requireContext().getString(R.string.notification_message_5),
            requireContext().getString(R.string.notification_message_6),
            requireContext().getString(R.string.notification_message_7),
            requireContext().getString(R.string.notification_message_8),
            requireContext().getString(R.string.notification_message_9),
            requireContext().getString(R.string.notification_message_10)
        )
        return messages.random()
    }

    override fun showNotificationSent() {
        // Handle notification sent event
    }

    override fun showNotificationScheduled() {
        // Handle notification scheduled event
    }

    override fun showNotificationCancelled() {
        // Handle notification cancelled event
    }

    private fun setupClickListeners() {
        // Add buttons for testing notifications
        viewBinding.btnTestNotification.setOnClickListener {
            presenter.testNotification()
            showImmediateNotification()
        }
        
        viewBinding.btnScheduleNotification.setOnClickListener {
            presenter.scheduleDailyNotification()
            scheduleDailyNotification()
        }
        
        viewBinding.btnCancelNotification.setOnClickListener {
            presenter.cancelDailyNotification()
            cancelDailyNotification()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, show notification immediately
                    showImmediateNotification()
                    scheduleDailyNotification()
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For older versions, show notification immediately
            showImmediateNotification()
            scheduleDailyNotification()
        }
    }

    private fun showImmediateNotification() {
        notificationManager.showImmediateNotification()
        Toast.makeText(context, "Test notification sent!", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleDailyNotification() {
        notificationService.scheduleDailyNotification(requireContext())
        Toast.makeText(context, "Daily notification scheduled for 12:00 PM!", Toast.LENGTH_LONG).show()
    }

    private fun cancelDailyNotification() {
        notificationService.cancelDailyNotification(requireContext())
        Toast.makeText(context, "Daily notification cancelled!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showTestNotificationOnLoad() {
        // Delay 1 giây để màn hình load xong rồi hiển thị thông báo
        viewBinding.root.postDelayed({
            showImmediateNotification()
            // Cập nhật nội dung hiển thị với thông báo mới
            val newMessage = getRandomNotificationMessage()
            viewBinding.textNotifications.text = newMessage
        }, 1000)
    }
}

