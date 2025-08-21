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
import com.example.cocktaildb.R
import com.example.cocktaildb.utils.AppNotificationManager
import com.example.cocktaildb.utils.base.BaseFragment

class NotificationsFragment : BaseFragment<FragmentNotificationsBinding>(), NotificationsContract.View {

	private lateinit var presenter: NotificationsPresenter
	private lateinit var appNotificationManager: AppNotificationManager
	private lateinit var notificationService: NotificationService

	private var hasShownInitialTest = false

	private val requestPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted: Boolean ->
		if (isGranted) {
			showImmediateNotification()
			scheduleDailyNotification()
		} else {
			Toast.makeText(context, getString(R.string.notif_permission_denied), Toast.LENGTH_SHORT).show()
		}
	}

	override fun inflateViewBinding(inflater: LayoutInflater): FragmentNotificationsBinding {
		return FragmentNotificationsBinding.inflate(inflater)
	}

	override fun initView() {
		presenter = NotificationsPresenter()
		presenter.setView(this)
		appNotificationManager = AppNotificationManager(requireContext())
		notificationService = NotificationService()
		setupClickListeners()
	}

	override fun initData() {
		presenter.onStart()
		presenter.loadNotifications()
		checkNotificationPermission()
		showTestNotificationOnLoad()
	}

	override fun onDestroyView() {
		presenter.onStop()
		super.onDestroyView()
	}

	override fun showNotifications() {
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

	override fun showNotificationSent() { }
	override fun showNotificationScheduled() { }
	override fun showNotificationCancelled() { }

	private fun setupClickListeners() {
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
					showImmediateNotificationOnce()
					scheduleDailyNotification()
				}
				else -> {
					requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
				}
			}
		} else {
			showImmediateNotificationOnce()
			scheduleDailyNotification()
		}
	}

	private fun showImmediateNotification() {
		appNotificationManager.showImmediateNotification()
		Toast.makeText(context, getString(R.string.notif_test_sent), Toast.LENGTH_SHORT).show()
	}

	private fun showImmediateNotificationOnce() {
		if (!hasShownInitialTest) {
			hasShownInitialTest = true
			showImmediateNotification()
		}
	}

	private fun scheduleDailyNotification() {
		notificationService.scheduleDailyNotification(requireContext())
		Toast.makeText(context, getString(R.string.notif_daily_scheduled), Toast.LENGTH_LONG).show()
	}

	private fun cancelDailyNotification() {
		notificationService.cancelDailyNotification(requireContext())
		Toast.makeText(context, getString(R.string.notif_daily_cancelled), Toast.LENGTH_SHORT).show()
	}

	private fun showTestNotificationOnLoad() {
		viewBinding.root.postDelayed({
			showImmediateNotificationOnce()
			val newMessage = getRandomNotificationMessage()
			viewBinding.textNotifications.text = newMessage
		}, TEST_NOTIFICATION_DELAY_MS)
	}

	companion object {
		private const val TEST_NOTIFICATION_DELAY_MS = 1000L
	}
}

