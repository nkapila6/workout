package com.nkapila.workout.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nkapila.workout.data.di.Graph
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.Routine
import com.nkapila.workout.data.openrouter.GenerationException
import com.nkapila.workout.data.openrouter.toRoutineAndExercises
import com.nkapila.workout.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class GenerateUiState {
    data class Input(
        val constraints: String = "",
        val model: String = "anthropic/claude-3.5-sonnet"
    ) : GenerateUiState()

    data object Loading : GenerateUiState()
    data class Preview(
        val routine: Routine,
        val exercises: List<Exercise>
    ) : GenerateUiState()

    data class Error(val message: String) : GenerateUiState()
}

class GenerateViewModel(
    private val repository: com.nkapila.workout.data.repo.WorkoutRepository = Graph.appContainer.repository,
    settingsRepository: com.nkapila.workout.data.settings.SettingsRepository = Graph.appContainer.settingsRepository,
    private val generator: com.nkapila.workout.data.openrouter.OpenRouterGenerator = Graph.appContainer.generator
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow<GenerateUiState>(GenerateUiState.Input())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    fun setConstraints(value: String) {
        if (_uiState.value is GenerateUiState.Input) {
            _uiState.value = (_uiState.value as GenerateUiState.Input).copy(constraints = value)
        }
    }

    fun setModel(value: String) {
        if (_uiState.value is GenerateUiState.Input) {
            _uiState.value = (_uiState.value as GenerateUiState.Input).copy(model = value)
        }
    }

    fun generate() {
        val state = _uiState.value as? GenerateUiState.Input ?: return
        _uiState.value = GenerateUiState.Loading

        viewModelScope.launch {
            try {
                val dto = generator.generateRoutine(
                    userConstraints = state.constraints,
                    model = state.model
                )
                val routineId = "custom_${System.currentTimeMillis()}"
                val (routine, exercises) = dto.toRoutineAndExercises(routineId)
                _uiState.value = GenerateUiState.Preview(routine, exercises)
            } catch (e: GenerationException) {
                val message = when (e.message) {
                    "invalid_key" -> "That key didn't work. Check it in Settings."
                    "network" -> "Network error. Try again."
                    "malformed" -> "Generation failed. Try again."
                    "no_key" -> "No API key set. Add one in Settings."
                    else -> "Generation failed. Try again."
                }
                _uiState.value = GenerateUiState.Error(message)
            } catch (e: Exception) {
                _uiState.value = GenerateUiState.Error("Generation failed. Try again.")
            }
        }
    }

    fun save() {
        val state = _uiState.value as? GenerateUiState.Preview ?: return
        viewModelScope.launch {
            state.exercises.forEach { repository.upsertExercise(it) }
            repository.saveRoutine(state.routine)
        }
    }

    fun discard() {
        _uiState.value = GenerateUiState.Input()
    }

    fun retry() {
        generate()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GenerateViewModel() as T
            }
        }
    }
}
