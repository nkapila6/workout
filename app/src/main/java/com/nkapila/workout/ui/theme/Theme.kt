package com.nkapila.workout.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF12151B),
    secondary = AmberDim,
    onSecondary = Ink,
    background = Bg,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    surfaceVariant = PanelAlt,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF12151B)
)

private val WorkoutShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun WorkoutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography(),
        shapes = WorkoutShapes,
        content = content
    )
}
