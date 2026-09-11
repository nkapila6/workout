package com.nkapila.workout.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkapila.workout.data.model.BodyWeightEntry
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.SessionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit = {},
    viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bodyWeights by viewModel.bodyWeights.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val history by viewModel.exerciseHistory.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
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
                ),
                scrollBehavior = scrollBehavior
            )
        },
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
            BodyWeightSection(
                units = settings?.units ?: "kg",
                entries = bodyWeights,
                onSave = viewModel::saveBodyWeight
            )

            ExerciseHistorySection(
                exercises = exercises,
                history = history,
                units = settings?.units ?: "kg",
                selectedId = viewModel.selectedExerciseId.collectAsStateWithLifecycle().value,
                onExerciseSelected = viewModel::selectExercise
            )
        }
    }
}

@Composable
private fun BodyWeightSection(
    units: String,
    entries: List<BodyWeightEntry>,
    onSave: (Double) -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val isLb = units == "lb"

    SectionCard(title = "Body weight") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { value -> input = value.filter { it.isDigit() || it == '.' } },
                label = { Text("Today's weight (${units})") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val parsed = input.toDoubleOrNull() ?: return@Button
                    val kg = if (isLb) parsed / 2.2046 else parsed
                    onSave(kg)
                    input = ""
                }
            ) {
                Text("Save")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            EmptyStateText("Log your first weigh-in")
        } else {
            BodyWeightChart(entries = entries, units = units)

            if (entries.size >= 3) {
                Spacer(modifier = Modifier.height(12.dp))
                WeeklyAverageDots(entries = entries, units = units)
            }
        }
    }
}

@Composable
private fun BodyWeightChart(entries: List<BodyWeightEntry>, units: String) {
    val sorted = remember(entries) { entries.sortedBy { it.date } }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    val (_, yMin, yMax) = remember(sorted, units) {
        val weights = sorted.map { it.weightKg.toDisplay(units) }
        val minWeight = weights.minOrNull() ?: 0.0
        val maxWeight = weights.maxOrNull() ?: 0.0
        val padding = max(1.0, (maxWeight - minWeight) * 0.12)
        Triple(sorted, (minWeight - padding).coerceAtLeast(0.0), maxWeight + padding)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingX = 32.dp.toPx()
            val paddingY = 24.dp.toPx()
            val chartWidth = width - paddingX * 2
            val chartHeight = height - paddingY * 2
            val dateRange = (sorted.last().date - sorted.first().date).toFloat().coerceAtLeast(1f)

            fun xFor(date: Long): Float =
                paddingX + (date - sorted.first().date).toFloat() / dateRange * chartWidth

            fun yFor(weight: Double): Float =
                paddingY + ((yMax - weight) / (yMax - yMin)).toFloat() * chartHeight

            for (i in 0..4) {
                val y = paddingY + chartHeight * i / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(paddingX, y),
                    end = Offset(width - paddingX, y),
                    strokeWidth = 1f
                )
            }

            val path = Path().apply {
                sorted.forEachIndexed { index, entry ->
                    val x = xFor(entry.date)
                    val y = yFor(entry.weightKg.toDisplay(units))
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx())
            )

            sorted.forEach { entry ->
                val center = Offset(xFor(entry.date), yFor(entry.weightKg.toDisplay(units)))
                drawCircle(
                    color = MaterialTheme.colorScheme.background,
                    radius = 5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = center
                )
            }
        }
    }

    if (entries.isNotEmpty()) {
        Text(
            text = "${entries.first().weightKg.toDisplayString(units)} → ${entries.last().weightKg.toDisplayString(units)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun WeeklyAverageDots(entries: List<BodyWeightEntry>, units: String) {
    val averages = remember(entries) {
        entries
            .groupBy { entry ->
                val date = java.time.Instant.ofEpochMilli(entry.date)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear().getFrom(date)
            }
            .map { (_, weekEntries) ->
                weekEntries.map { it.weightKg }.average() to weekEntries.first().date
            }
            .sortedBy { it.second }
    }

    Column {
        Text(
            text = "Weekly averages",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            averages.forEach { (avg, date) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                    Text(
                        text = avg.toDisplayString(units),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatShortDate(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseHistorySection(
    exercises: List<Exercise>,
    history: List<SessionLog>,
    units: String,
    selectedId: String,
    onExerciseSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = remember(exercises, selectedId) {
        exercises.find { it.id == selectedId } ?: exercises.firstOrNull()
    }

    SectionCard(title = "Exercise history") {
        if (exercises.isEmpty()) {
            EmptyStateText("No exercises yet")
            return@SectionCard
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Exercise") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                exercises.forEach { exercise ->
                    DropdownMenuItem(
                        text = { Text(exercise.name) },
                        onClick = {
                            onExerciseSelected(exercise.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            EmptyStateText("No logged sets for this exercise")
        } else {
            ExerciseBestWeightChart(history = history, units = units)
            Spacer(modifier = Modifier.height(12.dp))
            ExerciseHistoryList(history = history, units = units)
        }
    }
}

@Composable
private fun ExerciseBestWeightChart(history: List<SessionLog>, units: String) {
    val data = remember(history) {
        history
            .sortedBy { it.date }
            .mapNotNull { session ->
                val best = session.entries.maxByOrNull { it.weightKg } ?: return@mapNotNull null
                session.date to best.weightKg.toDisplay(units)
            }
    }

    if (data.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val minWeight = data.minOf { it.second }
    val maxWeight = data.maxOf { it.second }
    val padding = max(1.0, (maxWeight - minWeight) * 0.12)
    val yMin = (minWeight - padding).coerceAtLeast(0.0)
    val yMax = maxWeight + padding

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 160.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingX = 24.dp.toPx()
            val paddingY = 20.dp.toPx()
            val chartWidth = width - paddingX * 2
            val chartHeight = height - paddingY * 2
            val dateRange = (data.last().first - data.first().first).toFloat().coerceAtLeast(1f)

            fun xFor(date: Long): Float =
                paddingX + (date - data.first().first).toFloat() / dateRange * chartWidth

            fun yFor(weight: Double): Float =
                paddingY + ((yMax - weight) / (yMax - yMin)).toFloat() * chartHeight

            for (i in 0..3) {
                val y = paddingY + chartHeight * i / 3f
                drawLine(gridColor, Offset(paddingX, y), Offset(width - paddingX, y), strokeWidth = 1f)
            }

            val path = Path().apply {
                data.forEachIndexed { index, point ->
                    val x = xFor(point.first)
                    val y = yFor(point.second)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(path, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))

            data.forEach { point ->
                val center = Offset(xFor(point.first), yFor(point.second))
                drawCircle(MaterialTheme.colorScheme.background, 5.dp.toPx(), center)
                drawCircle(lineColor, 4.dp.toPx(), center)
            }
        }
    }
}

@Composable
private fun ExerciseHistoryList(history: List<SessionLog>, units: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        history.sortedByDescending { it.date }.forEach { session ->
            val sets = session.entries
            if (sets.isEmpty()) return@forEach
            val best = sets.maxBy { it.weightKg }
            val reps = sets.map { it.reps }
            val repsText = reps.joinToString(",")
            Text(
                text = "${formatShortDate(session.date)} - ${best.weightKg.toDisplayString(units)} x $repsText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EmptyStateText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun Double.toDisplay(units: String): Double =
    if (units == "lb") this * 2.2046 else this

private fun Double.toDisplayString(units: String): String =
    String.format(Locale.getDefault(), "%.1f%s", toDisplay(units), units)

private fun formatShortDate(timestamp: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
