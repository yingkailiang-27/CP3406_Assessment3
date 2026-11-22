package edu.algosteps.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import edu.algosteps.AlgoStepsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class DeviceReminders(private val context: Context) : ReminderScheduler {
    private val notifications = requireNotNull(context.getSystemService(NotificationManager::class.java))

    override suspend fun update(enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        val operation = if (enabled) {
            notifications.createNotificationChannel(
                NotificationChannel(
                    ReviewWorker.CHANNEL_ID, "Practice reminders", NotificationManager.IMPORTANCE_LOW
                ).apply { description = "An optional reminder to revisit an unfinished exercise." }
            )
            val request = PeriodicWorkRequestBuilder<ReviewWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .build()
            manager.enqueueUniquePeriodicWork("review-unfinished-practice", ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            manager.cancelUniqueWork("review-unfinished-practice")
        }
        withContext(Dispatchers.IO) { operation.result.get() }
    }

    override fun dismiss() = notifications.cancel(ReviewWorker.NOTIFICATION_ID)
}

class ReviewWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        if (!manager.areNotificationsEnabled() ||
            manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        ) return Result.success()

        val repository = (context as AlgoStepsApp).container.repository
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return Result.success()
        val tap = PendingIntent.getActivity(
            context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ready to try that exercise again?")
            .setContentText("An unfinished coding exercise is waiting in your practice history.")
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        repository.deliverReviewIfDue {
            try {
                manager.notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Device permission may change independently of in-app consent.
            }
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "review-reminders"
        const val NOTIFICATION_ID = 1704
    }
}
