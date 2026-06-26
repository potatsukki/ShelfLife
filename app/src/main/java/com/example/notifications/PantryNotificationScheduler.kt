package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.SettingsState
import java.util.concurrent.TimeUnit

object PantryNotificationScheduler {
    const val CHANNEL_ID = "pantry_alerts"
    private const val WORK_NAME = "pantry_notification_daily"

    fun schedule(context: Context) {
        createNotificationChannel(context)
        val request = PeriodicWorkRequestBuilder<PantryNotificationWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    fun refresh(context: Context, settings: SettingsState) {
        if (settings.expirationAlerts || settings.lowStockAlerts) {
            schedule(context)
        } else {
            cancel(context)
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pantry alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily reminders for pantry expiration and low stock."
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
