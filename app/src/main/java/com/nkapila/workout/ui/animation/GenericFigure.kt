package com.nkapila.workout.ui.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.nkapila.workout.ui.theme.Amber
import com.nkapila.workout.ui.theme.Ink
import com.nkapila.workout.ui.theme.Line
import com.nkapila.workout.ui.theme.Muted

/**
 * Static placeholder for exercises that do not have a custom animation.
 * Shows a standing stick figure, a subtle amber torso highlight, and the
 * muscle group label centered below the figure.
 */
@Composable
internal fun GenericFigure(
    muscleGroup: String,
    modifier: Modifier = Modifier,
    inkColor: Color = Ink,
    amberColor: Color = Amber,
    mutedColor: Color = Muted,
    lineColor: Color = Line
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewBoxWidth = 120f
            val viewBoxHeight = 140f
            val scale = size.minDimension / minOf(viewBoxWidth, viewBoxHeight)
            val dx = (size.width - viewBoxWidth * scale) / 2f
            val dy = (size.height - viewBoxHeight * scale) / 2f

            drawWithViewBox(scale, dx, dy, inkColor, amberColor, mutedColor, lineColor)
        }

        Text(
            text = muscleGroup,
            modifier = Modifier.align(Alignment.BottomCenter),
            color = mutedColor,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun DrawScope.drawWithViewBox(
    scale: Float,
    dx: Float,
    dy: Float,
    inkColor: Color,
    amberColor: Color,
    mutedColor: Color,
    lineColor: Color
) {
    fun pt(x: Float, y: Float) = Offset(dx + x * scale, dy + y * scale)

    // Floor line.
    val floor = pt(20f, 126f) to pt(100f, 126f)
    drawLine(lineColor, floor.first, floor.second, strokeWidth = 3f * scale)

    // Subtle amber highlight behind the torso.
    drawCircle(
        color = amberColor.copy(alpha = 0.14f),
        radius = 20f * scale,
        center = pt(60f, 72f)
    )

    val stroke = 4f * scale

    // Head.
    drawCircle(
        color = inkColor,
        radius = 9f * scale,
        center = pt(60f, 30f)
    )

    // Torso.
    drawLine(inkColor, pt(60f, 40f), pt(60f, 82f), strokeWidth = stroke)

    // Arms (relaxed at sides).
    drawLine(inkColor, pt(60f, 50f), pt(48f, 78f), strokeWidth = stroke)
    drawLine(inkColor, pt(60f, 50f), pt(72f, 78f), strokeWidth = stroke)

    // Legs.
    drawLine(inkColor, pt(60f, 82f), pt(52f, 126f), strokeWidth = stroke)
    drawLine(inkColor, pt(60f, 82f), pt(68f, 126f), strokeWidth = stroke)
}
