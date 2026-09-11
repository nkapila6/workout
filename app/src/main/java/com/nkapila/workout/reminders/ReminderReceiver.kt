package com.nkapila.workout.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nkapila.workout.MainActivity
import com.nkapila.workout.R
import com.nkapila.workout.data.di.Graph
import com.nkapila.workout.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val day = intent.getIntExtra(ReminderScheduler.EXTRA_DAY, 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                showNotification(context)

                if (day in 1..7 && Graph.appContainerIsInitialized()) {
                    val settings = withTimeoutOrNull(5_000) {
                        Graph.appContainer.settingsRepository.settings
                    }?.firstOrNull()
                    if (settings != null && settings.remindersEnabled) {
                        ReminderScheduler.scheduleSingleDay(context, day, settings)
                    }
                }
            }.onFailure {
                android.util.Log.e("ReminderReceiver", "Failed to handle reminder", it)
            }
            pendingResult.finish()
        }
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Workout session reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Strength session")
            .setContentText("Go light, learn the moves. Time to lift.")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "reminders"
        private const val NOTIFICATION_ID = 1001
    }
}

private fun Graph.appContainerIsInitialized(): Boolean {
    return try {
        Graph.appContainer != null
    } catch (e: UninitializedPropertyAccessException) {
        false
    }
}
