package com.example.cocktaildb.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager as SystemNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import java.util.Random

class AppNotificationManager(private val context: Context) {

	companion object {
		const val CHANNEL_ID = "cocktail_notifications"
	}

	private val random = Random()

	init {
		createNotificationChannel()
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val name = context.getString(R.string.notif_channel_name)
			val descriptionText = context.getString(R.string.notif_channel_description)
			val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
			val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
				description = descriptionText
				enableVibration(true)
				enableLights(true)
			}
			val notificationManager = context.getSystemService(SystemNotificationManager::class.java)
			notificationManager.createNotificationChannel(channel)
		}
	}

	fun showImmediateNotification() {
		val messages = listOf(
			context.getString(R.string.notification_message_1),
			context.getString(R.string.notification_message_2),
			context.getString(R.string.notification_message_3),
			context.getString(R.string.notification_message_4),
			context.getString(R.string.notification_message_5),
			context.getString(R.string.notification_message_6),
			context.getString(R.string.notification_message_7),
			context.getString(R.string.notification_message_8),
			context.getString(R.string.notification_message_9),
			context.getString(R.string.notification_message_10)
		)
		val randomMessage = messages[random.nextInt(messages.size)]
		showNotification(randomMessage, context.getString(R.string.notif_default_title))
	}

	fun showNotification(message: String, title: String) {
		val intent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		}
		val pendingIntent = PendingIntent.getActivity(
			context,
			0,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Notification.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_notifications_black_24dp)
				.setContentTitle(title)
				.setContentText(message)
				.setContentIntent(pendingIntent)
				.setAutoCancel(true)
				.build()
		} else {
			@Suppress("DEPRECATION")
			Notification.Builder(context)
				.setSmallIcon(R.drawable.ic_notifications_black_24dp)
				.setContentTitle(title)
				.setContentText(message)
				.setContentIntent(pendingIntent)
				.setAutoCancel(true)
				.build()
		}

		try {
			val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as SystemNotificationManager
			val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
			notificationManager.notify(notificationId, notification)
		} catch (e: SecurityException) {
			// Silently ignore if permission not granted
		}
	}
} 