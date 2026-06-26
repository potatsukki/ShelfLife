package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppSettingsStore
import com.example.data.Ingredient
import com.example.data.ShelfLifeAiService
import com.example.data.ShelfLifeDatabase
import com.example.data.ShelfLifeRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class PantryNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (userId.isBlank()) return Result.success()

        val settings = AppSettingsStore(applicationContext).settings.first()
        if (!settings.expirationAlerts && !settings.lowStockAlerts) return Result.success()

        val repository = ShelfLifeRepository(
            ShelfLifeDatabase.getDatabase(applicationContext).dao,
            ShelfLifeAiService()
        )
        val pantry = repository.getIngredientSnapshot(userId)
        val expired = if (settings.expirationAlerts) {
            pantry.filter { it.hasTrackedDate && daysUntil(it.expirationDate) < 0 }
        } else {
            emptyList()
        }
        val expiringSoon = if (settings.expirationAlerts) {
            pantry.filter { it.hasTrackedDate && daysUntil(it.expirationDate) in 0..3 }
        } else {
            emptyList()
        }
        val lowStock = if (settings.lowStockAlerts) {
            pantry.filter { it.lowStockReminderEnabled && it.quantity <= it.lowStockThreshold }
        } else {
            emptyList()
        }

        if (expired.isEmpty() && expiringSoon.isEmpty() && lowStock.isEmpty()) return Result.success()
        if (!hasNotificationPermission()) return Result.success()

        PantryNotificationScheduler.createNotificationChannel(applicationContext)
        NotificationManagerCompat.from(applicationContext).notify(
            NOTIFICATION_ID,
            buildNotification(expiringSoon, expired, lowStock)
        )
        return Result.success()
    }

    private fun buildNotification(
        expiringSoon: List<Ingredient>,
        expired: List<Ingredient>,
        lowStock: List<Ingredient>
    ) = NotificationCompat.Builder(applicationContext, PantryNotificationScheduler.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("ShelfLife Pantry Check")
        .setContentText(summaryText(expiringSoon.size, expired.size, lowStock.size))
        .setStyle(NotificationCompat.BigTextStyle().bigText(bigText(expiringSoon, expired, lowStock)))
        .setContentIntent(appPendingIntent())
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    private fun appPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(applicationContext, 0, intent, flags)
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun summaryText(expiringSoon: Int, expired: Int, lowStock: Int): String {
        return listOfNotNull(
            expiringSoon.takeIf { it > 0 }?.let { "$it ${if (it == 1) "item" else "items"} expiring soon" },
            expired.takeIf { it > 0 }?.let { "$it ${if (it == 1) "expired item" else "expired items"}" },
            lowStock.takeIf { it > 0 }?.let { "$it ${if (it == 1) "item is" else "items"} low stock" }
        ).joinToString(", ").replaceFirstChar { it.uppercaseChar() } + "."
    }

    private fun bigText(
        expiringSoon: List<Ingredient>,
        expired: List<Ingredient>,
        lowStock: List<Ingredient>
    ): String {
        val previews = listOfNotNull(
            previewLine("Expiring soon", expiringSoon),
            previewLine("Expired", expired),
            previewLine("Low stock", lowStock)
        )
        return "${summaryText(expiringSoon.size, expired.size, lowStock.size)}\n${previews.joinToString("\n")}"
    }

    private fun previewLine(label: String, items: List<Ingredient>): String? {
        if (items.isEmpty()) return null
        val preview = items.take(3).joinToString(", ") { it.name }
        val extra = (items.size - 3).takeIf { it > 0 }?.let { " +$it more" }.orEmpty()
        return "$label: $preview$extra"
    }

    private fun daysUntil(date: String): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val target = format.parse(date) ?: return 0
            val today = format.parse(format.format(Date())) ?: return 0
            ((target.time - today.time) / MILLIS_PER_DAY).toInt()
        } catch (_: Exception) {
            0
        }
    }

    private companion object {
        const val NOTIFICATION_ID = 4101
        const val MILLIS_PER_DAY = 1000 * 60 * 60 * 24
    }
}
