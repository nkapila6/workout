package com.nkapila.workout.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkapila.workout.data.di.Graph
import com.nkapila.workout.data.seed.SeedData
import com.nkapila.workout.ui.navigation.Routes
import com.nkapila.workout.ui.navigation.sessionRoute
import com.nkapila.workout.ui.theme.Amber
import com.nkapila.workout.ui.theme.AmberDim
import com.nkapila.workout.ui.theme.Bg
import com.nkapila.workout.ui.theme.Ink
import com.nkapila.workout.ui.theme.Line
import com.nkapila.workout.ui.theme.Muted
import com.nkapila.workout.ui.theme.Panel

@Composable
fun HomeScreen(
    onStartSession: (String) -> Unit,
    onProgress: () -> Unit,
    onGenerate: () -> Unit,
    onSettings: () -> Unit,
) {
    val routines by Graph.appContainer.repository.observeRoutines()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val sessionsThisWeek by Graph.appContainer.repository.sessionsInCurrentWeek()
        .collectAsStateWithLifecycle(initialValue = 0)

    val routine = routines.firstOrNull()
    val routineName = routine?.name ?: "Full-body strength"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Muted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = "FULL BODY - DUMBBELLS ONLY",
            color = Amber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.14.em,
            style = TextStyle(textAlign = TextAlign.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The repeat routine",
            color = Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.02).em,
            lineHeight = 1.15.em
        )
        Text(
            text = "Same five moves every session. No deciding, no planning. You show up and repeat. That is the whole point.",
            color = Muted,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(value = "2-3x", label = "PER WEEK", modifier = Modifier.weight(1f))
            StatCard(value = "~25", label = "MINUTES", modifier = Modifier.weight(1f))
            StatCard(value = "3x8-12", label = "SETS x REPS", modifier = Modifier.weight(1f))
        }

        WeeklyProgress(sessionsThisWeek)

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { onStartSession(routine?.id ?: Routes.DEFAULT_ROUTINE_ID) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber,
                contentColor = Bg
            )
        ) {
            Text(
                text = "Start session",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Bg
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        QuickLinks(onProgress = onProgress, onGenerate = onGenerate)

        Spacer(modifier = Modifier.height(12.dp))

        WarmupPreview(onStart = { onStartSession(routine?.id ?: Routes.DEFAULT_ROUTINE_ID) })

        Text(
            text = "Today: $routineName",
            color = Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 18.dp, start = 2.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Line)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Amber,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 0.03.em,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WeeklyProgress(sessionsThisWeek: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "This week: $sessionsThisWeek of 3 sessions",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    val filled = index < sessionsThisWeek.coerceIn(0, 3)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (filled) Amber else Line)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickLinks(onProgress: () -> Unit, onGenerate: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CompactLinkCard(
            label = "Progress",
            onClick = onProgress,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        CompactLinkCard(
            label = "New routine with AI",
            onClick = onGenerate,
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactLinkCard(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WarmupPreview(onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Amber.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Amber)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "WARM-UP",
                        color = Bg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.1.em
                    )
                }
                Text(
                    text = "3-4 min · no weight",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "Gets blood into the joints and rehearses the moves before you load them.",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SeedData.WARMUP.forEach { step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = step.count.toString().padStart(2, '0'),
                            color = Amber,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(28.dp)
                        )
                        Column {
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

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Bg)
            ) {
                Text(text = "Start session", fontWeight = FontWeight.Bold)
            }
        }
    }
}
