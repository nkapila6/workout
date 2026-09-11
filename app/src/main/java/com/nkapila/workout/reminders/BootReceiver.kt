package com.nkapila.workout.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nkapila.workout.data.di.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if (Graph.appContainerIsInitialized()) {
                    val settings = withTimeoutOrNull(5_000) {
                        Graph.appContainer.settingsRepository.settings.first()
                    }
                    if (settings != null) {
                        ReminderScheduler.schedule(context, settings)
                    }
                }
            }.onFailure {
                android.util.Log.e("BootReceiver", "Failed to reschedule reminders", it)
            }
            pendingResult.finish()
        }
    }
}

private fun Graph.appContainerIsInitialized(): Boolean {
    return try {
        Graph.appContainer != null
    } catch (e: UninitializedPropertyAccessException) {
        false
    }
}
