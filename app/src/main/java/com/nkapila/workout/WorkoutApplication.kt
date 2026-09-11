package com.nkapila.workout

import android.app.Application
import com.nkapila.workout.data.di.Graph
import com.nkapila.workout.data.di.container
import com.nkapila.workout.data.di.initContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkoutApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initContainer(this)
        Graph.appContainer = container
        applicationScope.launch {
            Graph.appContainer.repository.seedIfEmpty()
        }
    }
}
