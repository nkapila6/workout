package com.nkapila.workout.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.Routine
import com.nkapila.workout.data.model.RoutineItem
import com.nkapila.workout.ui.animation.ExerciseFigure
import kotlinx.coroutines.launch

private val DEFAULT_MODELS = listOf(
    "anthropic/claude-3.5-sonnet",
    "openai/gpt-4o-mini",
    "meta-llama/llama-3.1-70b-instruct"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: GenerateViewModel = viewModel(factory = GenerateViewModel.Factory)
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (settings?.openRouterKey.isNullOrBlank()) {
                NoApiKeyState(onOpenSettings = onOpenSettings)
            } else {
                when (val state = uiState) {
                    is GenerateUiState.Input -> InputForm(
                        state = state,
                        onConstraintsChange = viewModel::setConstraints,
                        onModelChange = viewModel::setModel,
                        onGenerate = viewModel::generate
                    )

                    is GenerateUiState.Loading -> LoadingState()

                    is GenerateUiState.Preview -> RoutinePreview(
                        routine = state.routine,
                        exercises = state.exercises,
                        onSave = {
                            viewModel.save()
                            scope.launch {
                                snackbarHostState.showSnackbar("Saved")
                            }
                            onBack()
                        },
                        onDiscard = viewModel::discard
                    )

                    is GenerateUiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = viewModel::retry,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun NoApiKeyState(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Add your OpenRouter API key in Settings to generate routines",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Open Settings")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputForm(
    state: GenerateUiState.Input,
    onConstraintsChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onGenerate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var custom by remember(state.model) { mutableStateOf(state.model.takeUnless { it in DEFAULT_MODELS } ?: "") }
    var isCustom by remember(state.model) { mutableStateOf(state.model !in DEFAULT_MODELS) }

    OutlinedTextField(
        value = state.constraints,
        onValueChange = onConstraintsChange,
        label = { Text("Constraints / goals") },
        placeholder = { Text("e.g. no jumping, bad knees, 20 minutes max") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Model",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = state.model,
            onValueChange = {},
            readOnly = true,
            label = { Text("Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DEFAULT_MODELS.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        isCustom = false
                        onModelChange(model)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Custom...") },
                onClick = {
                    isCustom = true
                    expanded = false
                }
            )
        }
    }

    if (isCustom) {
        OutlinedTextField(
            value = custom,
            onValueChange = {
                custom = it
                onModelChange(it)
            },
            label = { Text("Custom model slug") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onGenerate,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Generate")
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Generating...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoutinePreview(
    routine: Routine,
    exercises: List<Exercise>,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = routine.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        exercises.forEachIndexed { index, exercise ->
            GeneratedExerciseCard(
                index = index,
                exercise = exercise,
                item = routine.items.find { it.exerciseId == exercise.id } ?: RoutineItem(
                    exerciseId = exercise.id,
                    sets = 3,
                    repMin = 8,
                    repMax = 12
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save routine")
            }
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.weight(1f)
            ) {
                Text("Discard")
            }
        }
    }
}

@Composable
private fun GeneratedExerciseCard(
    index: Int,
    exercise: Exercise,
    item: RoutineItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%02d", index + 1),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = exercise.muscleGroup.groupName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.sets} sets x ${item.repMin}-${item.repMax}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = exercise.cue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ExerciseFigure(
                animationKey = "generic",
                muscleGroup = exercise.muscleGroup.groupName,
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) {
                Text("Try again")
            }
            if (message.contains("key", ignoreCase = true)) {
                OutlinedButton(onClick = onOpenSettings) {
                    Text("Settings")
                }
            }
        }
    }
}
