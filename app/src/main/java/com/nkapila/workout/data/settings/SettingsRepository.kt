package com.nkapila.workout.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SETTINGS_FILE = "settings"
private const val SECURE_FILE = "secure_prefs"
private const val KEY_OPEN_ROUTER = "open_router_key"

data class AppSettings(
    val reminderTimeMinutesOfDay: Int = 480,
    val reminderDays: Set<Int> = setOf(1, 4, 6),
    val remindersEnabled: Boolean = false,
    val restSeconds: Int = 75,
    val units: String = "kg",
    val openRouterKey: String = "",
    val model: String = "anthropic/claude-3.5-sonnet"
)

class SettingsRepository(context: Context) {

    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(SETTINGS_FILE)
    }

    private val securePrefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse { throwable ->
        android.util.Log.w("SettingsRepository", "Encrypted prefs unavailable, falling back to plaintext", throwable)
        context.getSharedPreferences(SETTINGS_FILE + "_plain", Context.MODE_PRIVATE)
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            reminderTimeMinutesOfDay = prefs[Keys.REMINDER_TIME] ?: 480,
            reminderDays = parseDays(prefs[Keys.REMINDER_DAYS]),
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: false,
            restSeconds = prefs[Keys.REST_SECONDS] ?: 75,
            units = prefs[Keys.UNITS] ?: "kg",
            openRouterKey = securePrefs.getString(KEY_OPEN_ROUTER, "") ?: "",
            model = prefs[Keys.MODEL] ?: "anthropic/claude-3.5-sonnet"
        )
    }

    suspend fun setReminderTime(minutesOfDay: Int) {
        dataStore.edit { it[Keys.REMINDER_TIME] = minutesOfDay }
    }

    suspend fun setReminderDays(days: Set<Int>) {
        dataStore.edit { it[Keys.REMINDER_DAYS] = days.joinToString(",") }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun setRestSeconds(seconds: Int) {
        dataStore.edit { it[Keys.REST_SECONDS] = seconds }
    }

    suspend fun setUnits(units: String) {
        dataStore.edit { it[Keys.UNITS] = units }
    }

    fun setOpenRouterKey(key: String) {
        securePrefs.edit().putString(KEY_OPEN_ROUTER, key).apply()
    }

    suspend fun setModel(model: String) {
        dataStore.edit { it[Keys.MODEL] = model }
    }

    private object Keys {
        val REMINDER_TIME = intPreferencesKey("reminder_time")
        val REMINDER_DAYS = stringPreferencesKey("reminder_days")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REST_SECONDS = intPreferencesKey("rest_seconds")
        val UNITS = stringPreferencesKey("units")
        val MODEL = stringPreferencesKey("model")
    }
}

private fun parseDays(value: String?): Set<Int> {
    if (value.isNullOrBlank()) return setOf(1, 4, 6)
    return value.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .toSortedSet()
}
