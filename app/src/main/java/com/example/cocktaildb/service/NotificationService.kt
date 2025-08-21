package com.example.cocktaildb.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import com.example.cocktaildb.utils.AppNotificationManager
import com.example.cocktaildb.R

class NotificationService {

	companion object {
		const val ACTION_DAILY_NOTIFICATION = "com.example.cocktaildb.DAILY_NOTIFICATION"
		const val REQUEST_CODE = 100
		const val DEFAULT_HOUR = 12
		const val DEFAULT_MINUTE = 0
	}

	fun scheduleDailyNotification(context: Context) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
			action = ACTION_DAILY_NOTIFICATION
		}
		val pendingIntent = PendingIntent.getBroadcast(
			context,
			REQUEST_CODE,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		val calendar = Calendar.getInstance()
		calendar.set(Calendar.HOUR_OF_DAY, DEFAULT_HOUR)
		calendar.set(Calendar.MINUTE, DEFAULT_MINUTE)
		calendar.set(Calendar.SECOND, 0)
		calendar.set(Calendar.MILLISECOND, 0)

		if (calendar.timeInMillis <= System.currentTimeMillis()) {
			calendar.add(Calendar.DAY_OF_YEAR, 1)
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			alarmManager.setExactAndAllowWhileIdle(
				AlarmManager.RTC_WAKEUP,
				calendar.timeInMillis,
				pendingIntent
			)
		} else {
			alarmManager.setExact(
				AlarmManager.RTC_WAKEUP,
				calendar.timeInMillis,
				pendingIntent
			)
		}
	}

	fun cancelDailyNotification(context: Context) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
			action = ACTION_DAILY_NOTIFICATION
		}
		val pendingIntent = PendingIntent.getBroadcast(
			context,
			REQUEST_CODE,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
		alarmManager.cancel(pendingIntent)
	}
}

class DailyNotificationReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == NotificationService.ACTION_DAILY_NOTIFICATION) {
			val appNotificationManager = AppNotificationManager(context)
			val title = context.getString(R.string.notif_default_title)
			val message = context.getString(R.string.notif_daily_message)
			appNotificationManager.showNotification(message, title)
			// Reschedule the next exact alarm for tomorrow at the same time
			NotificationService().scheduleDailyNotification(context)
		}
	}
}

