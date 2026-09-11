package com.nkapila.workout.data.di

import android.content.Context
import com.nkapila.workout.data.db.WorkoutDatabase
import com.nkapila.workout.data.openrouter.OpenRouterGenerator
import com.nkapila.workout.data.openrouter.createOpenRouterService
import com.nkapila.workout.data.repo.WorkoutRepository
import com.nkapila.workout.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {
    val database by lazy { WorkoutDatabase.create(context) }
    val dao by lazy { database.workoutDao() }
    val settingsRepository by lazy { SettingsRepository(context) }
    val repository by lazy { WorkoutRepository(dao, settingsRepository) }
    val generator by lazy {
        OpenRouterGenerator(
            api = createOpenRouterService(),
            json = Json { ignoreUnknownKeys = true; isLenient = false },
            keyProvider = {
                runBlocking { settingsRepository.settings.first().openRouterKey }
            }
        )
    }
}

lateinit var container: AppContainer

fun initContainer(context: Context) {
    container = AppContainer(context)
}

object Graph {
    lateinit var appContainer: AppContainer
}
