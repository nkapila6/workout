package com.nkapila.workout.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import com.nkapila.workout.data.di.Graph
import com.nkapila.workout.data.model.Difficulty
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.MuscleGroup
import com.nkapila.workout.data.seed.SeedData
import com.nkapila.workout.ui.animation.ExerciseFigure
import com.nkapila.workout.ui.theme.Amber
import com.nkapila.workout.ui.theme.Bg
import com.nkapila.workout.ui.theme.Ink
import com.nkapila.workout.ui.theme.Line
import com.nkapila.workout.ui.theme.Muted
import com.nkapila.workout.ui.theme.Panel
import com.nkapila.workout.ui.theme.PanelAlt
import com.nkapila.workout.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

class ExerciseDetailViewModel(
    private val exerciseId: String,
    private val repository: com.nkapila.workout.data.repo.WorkoutRepository = Graph.appContainer.repository,
) : androidx.lifecycle.ViewModel() {
    suspend fun loadExercise(): Exercise? = repository.getExercise(exerciseId)
    suspend fun alternatives(group: MuscleGroup): List<Exercise> =
        repository.getExercisesForMuscleGroup(group.groupName)
            .filter { it.id != exerciseId }
            .sortedBy { it.difficulty.ordinal }

    suspend fun swapFor(selectedId: String): Boolean {
        return try {
            repository.swapExercise(SeedData.DEFAULT_ROUTINE_ID, exerciseId, selectedId)
            true
        } catch (_: Throwable) {
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
) {
    val viewModel: ExerciseDetailViewModel = viewModel(
        key = exerciseId,
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer {
                ExerciseDetailViewModel(exerciseId = exerciseId)
            }
        }
    )

    var exercise by remember { mutableStateOf<Exercise?>(null) }
    var alternatives by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var showSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(exerciseId) {
        val loaded = viewModel.loadExercise()
        exercise = loaded
        loaded?.let { alternatives = viewModel.alternatives(it.muscleGroup) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
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
                    text = "Exercise",
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (exercise == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Loading...", color = Muted)
                }
                return@Column
            }

            val ex = exercise!!

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, Line)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(PanelAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        ExerciseFigure(
                            animationKey = ex.animationKey,
                            muscleGroup = ex.muscleGroup.groupName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = ex.name,
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.02).em
                    )

                    DifficultyPill(difficulty = ex.difficulty)

                    Text(
                        text = ex.muscleGroup.groupName.replaceFirstChar { it.uppercase() },
                        color = Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                border = BorderStroke(1.dp, Line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Form cue",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = ex.cue,
                        color = Muted,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { showSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Line),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber)
            ) {
                Text(text = "Swap this exercise", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Bg)
            ) {
                Text(text = "Back to session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Bg,
            contentColor = Ink,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Swap for an alternative",
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )

                alternatives.forEach { alt ->
                    AlternativeRow(
                        exercise = alt,
                        onClick = {
                            scope.launch {
                                val swapped = viewModel.swapFor(alt.id)
                                if (swapped) {
                                    snackbarHostState.showSnackbar("Swapped ${exercise?.name ?: "exercise"} for ${alt.name}")
                                }
                                sheetState.hide()
                                showSheet = false
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (alternatives.isEmpty()) {
                    Text(
                        text = "No alternatives found for this muscle group.",
                        color = Muted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyPill(difficulty: Difficulty) {
    val color = when (difficulty) {
        Difficulty.EASIER -> Muted
        Difficulty.STANDARD -> Amber
        Difficulty.HARDER -> SuccessGreen
    }
    Box(
        modifier = Modifier
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = difficulty.value.replaceFirstChar { it.uppercase() },
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AlternativeRow(
    exercise: Exercise,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = exercise.cue,
                    color = Muted,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
            DifficultyChip(difficulty = exercise.difficulty)
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: Difficulty) {
    val color = when (difficulty) {
        Difficulty.EASIER -> Muted
        Difficulty.STANDARD -> Amber
        Difficulty.HARDER -> SuccessGreen
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = difficulty.value.replaceFirstChar { it.uppercase() },
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
