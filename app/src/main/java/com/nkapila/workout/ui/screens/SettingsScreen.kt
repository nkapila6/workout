package com.nkapila.workout.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val DEFAULT_MODELS = listOf(
    "anthropic/claude-3.5-sonnet",
    "openai/gpt-4o-mini",
    "meta-llama/llama-3.1-70b-instruct"
)

private val DAYS = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = viewModel(
        factory = remember(context) { SettingsViewModel.createFactory(context) }
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result is informational; Android still lets us schedule alarms.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val current = settings ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SectionTitle("Reminders")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enabled", color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = current.remindersEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        scope.launch {
                            viewModel.setRemindersEnabled(enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            if (current.remindersEnabled) {
                TimePickerRow(
                    minutesOfDay = current.reminderTimeMinutesOfDay,
                    onTimeChange = { minutes ->
                        scope.launch { viewModel.setReminderTime(minutes) }
                    }
                )

                Text(
                    text = "Days",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DAYS.forEach { (day, label) ->
                        FilterChip(
                            selected = day in current.reminderDays,
                            onClick = {
                                val updated = if (day in current.reminderDays) {
                                    current.reminderDays - day
                                } else {
                                    current.reminderDays + day
                                }
                                scope.launch { viewModel.setReminderDays(updated) }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            SectionTitle("Rest timer")
            Column {
                Text(
                    text = "${current.restSeconds}s",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = current.restSeconds.toFloat(),
                    onValueChange = { value ->
                        scope.launch { viewModel.setRestSeconds(value.toInt()) }
                    },
                    valueRange = 30f..180f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionTitle("Units")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("kg", "lb").forEach { unit ->
                    FilterChip(
                        selected = current.units == unit,
                        onClick = { scope.launch { viewModel.setUnits(unit) } },
                        label = { Text(unit.uppercase()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            SectionTitle("OpenRouter")
            ApiKeyField(
                value = current.openRouterKey,
                onValueChange = { key -> viewModel.setOpenRouterKey(key) }
            )
            ModelSelector(
                selected = current.model,
                onSelect = { model -> scope.launch { viewModel.setModel(model) } }
            )

            Text(
                text = "Your key is stored encrypted on this device only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun TimePickerRow(
    minutesOfDay: Int,
    onTimeChange: (Int) -> Unit
) {
    var hour by remember(minutesOfDay) { mutableStateOf((minutesOfDay / 60).coerceIn(0, 23)) }
    var minute by remember(minutesOfDay) { mutableStateOf(minutesOfDay % 60) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = hour.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.coerceIn(0, 23)?.let {
                    hour = it
                    onTimeChange(hour * 60 + minute)
                }
            },
            label = { Text("Hour") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Text(":", color = MaterialTheme.colorScheme.onBackground)
        OutlinedTextField(
            value = minute.toString().padStart(2, '0'),
            onValueChange = { value ->
                value.toIntOrNull()?.coerceIn(0, 59)?.let {
                    minute = it
                    onTimeChange(hour * 60 + minute)
                }
            },
            label = { Text("Minute") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("OpenRouter API key") },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) "Hide key" else "Show key"
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(selected.takeUnless { it in DEFAULT_MODELS } ?: "") }
    var isCustom by remember(selected) { mutableStateOf(selected !in DEFAULT_MODELS) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected,
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
                            onSelect(model)
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
                    onSelect(it)
                },
                label = { Text("Custom model slug") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
