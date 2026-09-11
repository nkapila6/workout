package com.nkapila.workout.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkapila.workout.data.di.Graph
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.Routine
import com.nkapila.workout.data.model.RoutineItem
import com.nkapila.workout.data.model.SetLog
import com.nkapila.workout.data.repo.WorkoutRepository
import com.nkapila.workout.data.seed.SeedData
import com.nkapila.workout.data.settings.AppSettings
import com.nkapila.workout.data.settings.SettingsRepository
import com.nkapila.workout.ui.animation.ExerciseFigure
import com.nkapila.workout.ui.theme.Amber
import com.nkapila.workout.ui.theme.AmberDim
import com.nkapila.workout.ui.theme.Bg
import com.nkapila.workout.ui.theme.Ink
import com.nkapila.workout.ui.theme.Line
import com.nkapila.workout.ui.theme.Muted
import com.nkapila.workout.ui.theme.Panel
import com.nkapila.workout.ui.theme.PanelAlt
import com.nkapila.workout.ui.theme.SuccessGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val routineId: String,
    private val repository: WorkoutRepository = Graph.appContainer.repository,
    private val settingsRepository: SettingsRepository = Graph.appContainer.settingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadRoutine()
    }

    private fun loadRoutine() {
        viewModelScope.launch {
            val routine = repository.getRoutine(routineId)
            if (routine == null) {
                _uiState.update { it.copy(phase = SessionPhase.ERROR, errorMessage = "Routine not found") }
                return@launch
            }
            val exerciseMap = mutableMapOf<String, Exercise>()
            val suggestions = mutableMapOf<String, SetLog?>()
            routine.items.forEach { item ->
                exerciseMap[item.exerciseId] = repository.getExercise(item.exerciseId) ?: return@forEach
                suggestions[item.exerciseId] = repository.lastEntryFor(item.exerciseId)
            }
            val entries = routine.items.associate { item ->
                item.exerciseId to List(item.sets) { setIndex ->
                    val suggestion = suggestions[item.exerciseId]
                    PendingSet(
                        setNumber = setIndex + 1,
                        weight = suggestion?.weightKg?.toString() ?: "",
                        reps = suggestion?.reps?.toString() ?: "",
                    )
                }.toMutableList()
            }
            _uiState.update {
                it.copy(
                    phase = SessionPhase.WARMUP,
                    routine = routine,
                    exercises = exerciseMap,
                    pendingEntries = entries,
                )
            }
        }
    }

    fun skipWarmup() {
        _uiState.update { it.copy(phase = SessionPhase.EXERCISE, warmupStepChecked = List(SeedData.WARMUP.size) { true }) }
    }

    fun toggleWarmupStep(index: Int) {
        _uiState.update { state ->
            val checked = state.warmupStepChecked.toMutableList().apply {
                this[index] = !this[index]
            }
            state.copy(warmupStepChecked = checked)
        }
    }

    fun beginWorkout() {
        _uiState.update { it.copy(phase = SessionPhase.EXERCISE) }
    }

    fun updatePendingSet(exerciseId: String, setNumber: Int, weight: String, reps: String) {
        _uiState.update { state ->
            val list = state.pendingEntries[exerciseId]?.toMutableList() ?: return@update state
            val idx = list.indexOfFirst { it.setNumber == setNumber }
            if (idx >= 0) {
                list[idx] = list[idx].copy(weight = weight, reps = reps)
            }
            state.copy(pendingEntries = state.pendingEntries.toMutableMap().apply { put(exerciseId, list) })
        }
    }

    fun logSet(exerciseId: String, setNumber: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val pending = state.pendingEntries[exerciseId]?.firstOrNull { it.setNumber == setNumber } ?: return@launch
            val weightKg = parseWeightKg(pending.weight, state.settings.units)
            val reps = pending.reps.toIntOrNull() ?: 0
            if (weightKg <= 0 || reps <= 0) return@launch

            _uiState.update { s ->
                val logged = s.loggedEntries[exerciseId]?.toMutableList() ?: mutableListOf()
                val existing = logged.indexOfFirst { it.setNumber == setNumber }
                val entry = SetLog(exerciseId, setNumber, reps, weightKg)
                if (existing >= 0) logged[existing] = entry else logged.add(entry)
                s.copy(loggedEntries = s.loggedEntries.toMutableMap().apply { put(exerciseId, logged) })
            }

            val item = state.routine?.items?.firstOrNull { it.exerciseId == exerciseId } ?: return@launch
            val loggedCount = state.loggedEntries[exerciseId]?.size?.plus(1) ?: 1
            if (loggedCount < item.sets) {
                startRestTimer()
            } else {
                checkProgression(exerciseId, item.repMax)
            }
        }
    }

    fun nextExercise() {
        _uiState.update { state ->
            val nextIndex = state.currentExerciseIndex + 1
            val exerciseIds = state.routine?.items?.map { it.exerciseId } ?: emptyList()
            if (nextIndex >= exerciseIds.size) {
                state.copy(phase = SessionPhase.SUMMARY)
            } else {
                state.copy(currentExerciseIndex = nextIndex)
            }
        }
    }

    fun previousExercise() {
        _uiState.update { state ->
            val prev = (state.currentExerciseIndex - 1).coerceAtLeast(0)
            state.copy(currentExerciseIndex = prev)
        }
    }

    fun startRestTimer() {
        timerJob?.cancel()
        val restSeconds = _uiState.value.settings.restSeconds
        _uiState.update { it.copy(restSecondsRemaining = restSeconds, showRestOverlay = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.restSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { s -> s.copy(restSecondsRemaining = (s.restSecondsRemaining - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun addRestSeconds(seconds: Int) {
        _uiState.update { it.copy(restSecondsRemaining = it.restSecondsRemaining + seconds) }
    }

    fun dismissRestOverlay() {
        timerJob?.cancel()
        _uiState.update { it.copy(showRestOverlay = false) }
    }

    fun finishSession(onComplete: suspend () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val allEntries = state.loggedEntries.values.flatten()
            repository.saveSession(allEntries, routineId)
            onComplete()
        }
    }

    fun abandon() {
        timerJob?.cancel()
        _uiState.update { SessionUiState() }
    }

    private suspend fun checkProgression(exerciseId: String, repMax: Int) {
        if (repository.shouldSuggestWeightIncrease(exerciseId, repMax)) {
            _uiState.update { it.copy(showProgressionPrompt = true) }
        }
    }

    fun dismissProgressionPrompt() {
        _uiState.update { it.copy(showProgressionPrompt = false) }
    }

    private fun parseWeightKg(input: String, units: String): Double {
        val value = input.toDoubleOrNull() ?: return 0.0
        return if (units == "lb") value / 2.2046 else value
    }

    fun refreshSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _uiState.update { it.copy(settings = settings) }
        }
    }
}

data class SessionUiState(
    val phase: SessionPhase = SessionPhase.LOADING,
    val routine: Routine? = null,
    val exercises: Map<String, Exercise> = emptyMap(),
    val currentExerciseIndex: Int = 0,
    val warmupStepChecked: List<Boolean> = List(SeedData.WARMUP.size) { false },
    val pendingEntries: Map<String, List<PendingSet>> = emptyMap(),
    val loggedEntries: Map<String, List<SetLog>> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val showRestOverlay: Boolean = false,
    val restSecondsRemaining: Int = 0,
    val showProgressionPrompt: Boolean = false,
    val errorMessage: String? = null,
)

enum class SessionPhase { LOADING, WARMUP, EXERCISE, SUMMARY, ERROR }

data class PendingSet(
    val setNumber: Int,
    val weight: String,
    val reps: String,
)

private const val LB_PER_KG = 2.2046

@Composable
fun SessionScreen(
    routineId: String,
    onExit: () -> Unit,
    onExerciseDetail: (String) -> Unit,
) {
    val viewModel: SessionViewModel = viewModel(
        key = routineId,
        factory = androidx.lifecycle.viewmodel.initializer.viewModelFactory {
            SessionViewModel(routineId = routineId)
        }
    )

    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        when (uiState.phase) {
            SessionPhase.LOADING -> LoadingOverlay()
            SessionPhase.ERROR -> ErrorOverlay(message = uiState.errorMessage ?: "Error")
            SessionPhase.WARMUP -> WarmupPhase(
                checked = uiState.warmupStepChecked,
                onToggle = viewModel::toggleWarmupStep,
                onSkip = viewModel::skipWarmup,
                onBegin = viewModel::beginWorkout,
                onBack = onExit,
            )
            SessionPhase.EXERCISE -> {
                val items = uiState.routine?.items ?: emptyList()
                val exerciseId = items.getOrNull(uiState.currentExerciseIndex)?.exerciseId
                val exercise = exerciseId?.let { uiState.exercises[it] }
                if (exercise != null && exerciseId != null) {
                    ExercisePhase(
                        item = items[uiState.currentExerciseIndex],
                        exercise = exercise,
                        exerciseId = exerciseId,
                        index = uiState.currentExerciseIndex,
                        total = items.size,
                        pendingSets = uiState.pendingEntries[exerciseId] ?: emptyList(),
                        loggedSets = uiState.loggedEntries[exerciseId] ?: emptyList(),
                        units = uiState.settings.units,
                        showProgressionPrompt = uiState.showProgressionPrompt,
                        onUpdateSet = viewModel::updatePendingSet,
                        onLogSet = viewModel::logSet,
                        onNext = viewModel::nextExercise,
                        onBack = viewModel::previousExercise,
                        onAbandon = onExit,
                        onExerciseDetail = onExerciseDetail,
                        onDismissProgressionPrompt = viewModel::dismissProgressionPrompt,
                    )
                }
            }
            SessionPhase.SUMMARY -> SummaryPhase(
                routine = uiState.routine,
                loggedEntries = uiState.loggedEntries,
                exercises = uiState.exercises,
                units = uiState.settings.units,
                onDone = {
                    viewModel.finishSession(onComplete = onExit)
                },
                onBack = onExit,
            )
        }

        if (uiState.showRestOverlay) {
            RestOverlay(
                secondsRemaining = uiState.restSecondsRemaining,
                totalSeconds = uiState.settings.restSeconds,
                onSkip = viewModel::dismissRestOverlay,
                onAdd15 = { viewModel.addRestSeconds(15) },
            )
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Amber)
    }
}

@Composable
private fun ErrorOverlay(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = Muted)
    }
}

@Composable
private fun WarmupPhase(
    checked: List<Boolean>,
    onToggle: (Int) -> Unit,
    onSkip: () -> Unit,
    onBegin: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TopBar(title = "Warm-up", onBack = onBack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Amber.copy(alpha = 0.06f)),
            border = BorderStroke(1.dp, Amber.copy(alpha = 0.22f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3-4 min · no weight",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tap each step once you have done it, then begin the workout.",
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SeedData.WARMUP.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = checked.getOrElse(index) { false },
                            onCheckedChange = { onToggle(index) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Amber,
                                uncheckedColor = Line,
                                checkmarkColor = Bg
                            )
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = step.label,
                                color = Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = step.detail,
                                color = Muted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted),
            border = BorderStroke(1.dp, Line)
        ) {
            Text(text = "Skip warm-up")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onBegin,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Bg)
        ) {
            Text(text = "Begin workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExercisePhase(
    item: RoutineItem,
    exercise: Exercise,
    exerciseId: String,
    index: Int,
    total: Int,
    pendingSets: List<PendingSet>,
    loggedSets: List<SetLog>,
    units: String,
    showProgressionPrompt: Boolean,
    onUpdateSet: (String, Int, String, String) -> Unit,
    onLogSet: (String, Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAbandon: () -> Unit,
    onExerciseDetail: (String) -> Unit,
    onDismissProgressionPrompt: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        TopBar(title = "Session", onBack = onAbandon)

        Text(
            text = "${index + 1} of $total",
            color = Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            border = BorderStroke(1.dp, Line)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelAlt),
                    contentAlignment = Alignment.Center
                ) {
                    ExerciseFigure(
                        animationKey = exercise.animationKey,
                        muscleGroup = exercise.muscleGroup.groupName,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    color = AmberDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.08.em
                )
                Text(
                    text = exercise.name,
                    color = Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 3.dp)
                )

                TagPill(text = exercise.muscleGroup.groupName)

                val rangeText = "${item.sets} sets x "
                val repText = "${item.repMin}-${item.repMax} reps"
                Row {
                    Text(text = rangeText, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = repText, color = Amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (item.perSide) {
                        Text(text = " each side", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    text = exercise.cue,
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (showProgressionPrompt) {
            ProgressionBanner(onDismiss = onDismissProgressionPrompt)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            border = BorderStroke(1.dp, Line)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Log sets",
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                pendingSets.forEach { pending ->
                    val logged = loggedSets.firstOrNull { it.setNumber == pending.setNumber }
                    SetLogRow(
                        pending = pending,
                        logged = logged,
                        units = units,
                        onUpdate = { weight, reps ->
                            onUpdateSet(exerciseId, pending.setNumber, weight, reps)
                        },
                        onLog = { onLogSet(exerciseId, pending.setNumber) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (loggedSets.isNotEmpty()) {
                    Text(
                        text = "Logged: ${loggedSets.joinToString(", ") { displayWeight(it.weightKg, units) + " x " + it.reps }}",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { onExerciseDetail(exerciseId) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted),
            border = BorderStroke(1.dp, Line)
        ) {
            Text(text = "Exercise details / swap")
        }

        Spacer(modifier = Modifier.height(10.dp))

        val allLogged = loggedSets.size >= item.sets
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Bg),
            enabled = allLogged
        ) {
            Text(
                text = if (index == total - 1) "Finish workout" else "Next exercise",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SetLogRow(
    pending: PendingSet,
    logged: SetLog?,
    units: String,
    onUpdate: (String, String) -> Unit,
    onLog: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Set ${pending.setNumber}",
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(60.dp)
        )

        NumberField(
            value = pending.weight,
            onValueChange = { onUpdate(it, pending.reps) },
            label = units.uppercase(),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        NumberField(
            value = pending.reps,
            onValueChange = { onUpdate(pending.weight, it) },
            label = "REPS",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))

        val isLogged = logged != null
        Button(
            onClick = onLog,
            enabled = !isLogged && pending.weight.isNotBlank() && pending.reps.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isLogged) SuccessGreen else Amber, contentColor = Bg),
            modifier = Modifier.width(80.dp)
        ) {
            if (isLogged) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Logged", modifier = Modifier.size(18.dp))
            } else {
                Text(text = "Log", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = Muted, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PanelAlt,
            unfocusedContainerColor = PanelAlt,
            focusedBorderColor = Amber,
            unfocusedBorderColor = Line,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(56.dp),
        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun ProgressionBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
    Text(
        text = "You hit 12s across the board. Consider adding weight next time.",
        color = SuccessGreen,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f)
    )
            IconButton(onClick = onDismiss) {
                Text(text = "OK", color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryPhase(
    routine: Routine?,
    loggedEntries: Map<String, List<SetLog>>,
    exercises: Map<String, Exercise>,
    units: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        TopBar(title = "Summary", onBack = onBack)

        Text(
            text = "Session complete",
            color = Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.02).em,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        routine?.items?.forEach { item ->
            val exercise = exercises[item.exerciseId]
            val sets = loggedEntries[item.exerciseId] ?: emptyList()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, Line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = exercise?.name ?: item.exerciseId,
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TagPill(text = exercise?.muscleGroup?.groupName ?: "")
                    if (sets.isNotEmpty()) {
                        Text(
                            text = sets.joinToString(", ") { "${displayWeight(it.weightKg, units)} x ${it.reps}" },
                            color = Amber,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Text(
                            text = "No sets logged",
                            color = Muted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Bg)
        ) {
            Text(text = "Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RestOverlay(
    secondsRemaining: Int,
    totalSeconds: Int,
    onSkip: () -> Unit,
    onAdd15: () -> Unit,
) {
    val progress = if (totalSeconds > 0) secondsRemaining / totalSeconds.toFloat() else 0f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Panel),
            border = BorderStroke(1.dp, Line)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Rest",
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatSeconds(secondsRemaining),
                    color = Amber,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Amber,
                    trackColor = Line,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSkip,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Line),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted)
                    ) {
                        Text(text = "Skip")
                    }
                    OutlinedButton(
                        onClick = onAdd15,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Line),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber)
                    ) {
                        Text(text = "+15s")
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Muted
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TagPill(text: String) {
    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, Line, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.replaceFirstChar { it.uppercase() },
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun displayWeight(weightKg: Double, units: String): String {
    val value = if (units == "lb") weightKg * LB_PER_KG else weightKg
    return "%.1f%s".format(value, units)
}
