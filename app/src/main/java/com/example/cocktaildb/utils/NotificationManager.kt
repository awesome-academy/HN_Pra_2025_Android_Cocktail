package com.example.cocktaildb.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cocktaildb.MainActivity
import com.example.cocktaildb.R
import java.util.Random

class NotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "cocktail_notifications"
        const val CHANNEL_NAME = "Cocktail Notifications"
        const val CHANNEL_DESCRIPTION = "Notifications for cocktail app"
        const val NOTIFICATION_ID = 1
    }

    private val random = Random()

    // Danh sách các thông báo mẫu để random
    private val notificationMessages = listOf(
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

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Hiển thị thông báo ngay lập tức (để test)
    fun showImmediateNotification() {
        val randomMessage = getRandomNotificationMessage()
        showNotification(randomMessage, "Cocktail App")
    }

    // Hiển thị thông báo với tiêu đề và nội dung tùy chỉnh
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            // Log error instead of printing stack trace
        }
    }

    // Lấy thông báo ngẫu nhiên từ danh sách
    private fun getRandomNotificationMessage(): String {
        return notificationMessages[random.nextInt(notificationMessages.size)]
    }

    // Lên lịch thông báo định kỳ vào 12h trưa
    fun scheduleDailyNotification() {
        // Sẽ được implement trong NotificationService
    }
} 