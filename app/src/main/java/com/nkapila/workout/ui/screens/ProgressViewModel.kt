package com.nkapila.workout.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nkapila.workout.data.di.Graph
import com.nkapila.workout.data.model.BodyWeightEntry
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.SessionLog
import com.nkapila.workout.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val repository: com.nkapila.workout.data.repo.WorkoutRepository = Graph.appContainer.repository,
    settingsRepository: com.nkapila.workout.data.settings.SettingsRepository = Graph.appContainer.settingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bodyWeights: StateFlow<List<BodyWeightEntry>> = repository.observeBodyWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercises: StateFlow<List<Exercise>> = repository.observeAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedExerciseId = MutableStateFlow("")
    val selectedExerciseId: StateFlow<String> = _selectedExerciseId.asStateFlow()

    val exerciseHistory: StateFlow<List<SessionLog>> = _selectedExerciseId
        .flatMapLatest { id ->
            if (id.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.observeExerciseHistory(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBodyWeight(weightKg: Double) {
        viewModelScope.launch {
            repository.saveBodyWeight(weightKg)
        }
    }

    fun selectExercise(exerciseId: String) {
        _selectedExerciseId.value = exerciseId
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgressViewModel() as T
            }
        }
    }
}
