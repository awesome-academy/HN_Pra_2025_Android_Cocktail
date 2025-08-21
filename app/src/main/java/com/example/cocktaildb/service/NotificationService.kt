package com.example.cocktaildb.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.example.cocktaildb.utils.NotificationManager
import java.util.Calendar

class NotificationService {

    companion object {
        const val ACTION_DAILY_NOTIFICATION = "com.example.cocktaildb.DAILY_NOTIFICATION"
        const val REQUEST_CODE = 100
    }

    // Lên lịch thông báo hàng ngày vào 12h trưa
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

        // Tính toán thời gian 12h trưa hôm nay
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Nếu đã qua 12h trưa hôm nay, lên lịch cho ngày mai
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Lên lịch thông báo định kỳ
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

        // Lên lịch thông báo lặp lại hàng ngày
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    // Hủy lịch thông báo
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

// BroadcastReceiver để nhận thông báo định kỳ
class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationService.ACTION_DAILY_NOTIFICATION) {
            val notificationManager = NotificationManager(context)
            notificationManager.showNotification(
                "🍹 Thời gian thưởng thức cocktail rồi! Khám phá công thức mới nào?",
                "Cocktail App"
            )
        }
    }
}

