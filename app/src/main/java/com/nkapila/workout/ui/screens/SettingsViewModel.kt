package com.nkapila.workout.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nkapila.workout.data.di.Graph
import com.nkapila.workout.data.settings.AppSettings
import com.nkapila.workout.reminders.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: com.nkapila.workout.data.settings.SettingsRepository = Graph.appContainer.settingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun setReminderTime(minutesOfDay: Int) {
        settingsRepository.setReminderTime(minutesOfDay)
        reschedule()
    }

    suspend fun setReminderDays(days: Set<Int>) {
        settingsRepository.setReminderDays(days)
        reschedule()
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        settingsRepository.setRemindersEnabled(enabled)
        reschedule()
    }

    suspend fun setRestSeconds(seconds: Int) {
        settingsRepository.setRestSeconds(seconds.coerceIn(30, 180))
    }

    suspend fun setUnits(units: String) {
        settingsRepository.setUnits(units)
    }

    fun setOpenRouterKey(key: String) {
        settingsRepository.setOpenRouterKey(key)
    }

    suspend fun setModel(model: String) {
        settingsRepository.setModel(model)
    }

    private suspend fun reschedule() {
        val current = settings.value ?: return
        ReminderScheduler.schedule(context, current)
    }

    companion object {
        fun createFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(context = context.applicationContext) as T
                }
            }
        }
    }
}
