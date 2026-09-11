package com.nkapila.workout.ui.animation

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import com.nkapila.workout.ui.theme.Amber
import com.nkapila.workout.ui.theme.Ink
import com.nkapila.workout.ui.theme.Line

private const val VIEW_BOX_WIDTH = 120f
private const val VIEW_BOX_HEIGHT = 140f

/**
 * Animated exercise stick figure. Renders one of five built-in figures
 * matching the SVG SMIL animations in home-strength-routine.html, or a
 * static generic placeholder for unknown keys.
 *
 * @param animationKey one of "goblet_squat", "floor_press", "one_arm_row",
 *        "romanian_deadlift", "shoulder_press", or any other string for
 *        the generic fallback.
 * @param muscleGroup label shown under the generic placeholder.
 */
@Composable
fun ExerciseFigure(
    animationKey: String,
    muscleGroup: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduceMotion = remember { context.isAnimatorDurationScaleZero() }

    val spec = figures[animationKey]
    if (spec == null) {
        GenericFigure(muscleGroup = muscleGroup, modifier = modifier)
        return
    }

    val phase = if (reduceMotion) {
        remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = animationKey)
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(spec.periodMillis, easing = LinearEasing)
            ),
            label = animationKey
        )
    }

    Canvas(modifier = modifier) {
        val scale = size.minDimension / minOf(VIEW_BOX_WIDTH, VIEW_BOX_HEIGHT)
        val dx = (size.width - VIEW_BOX_WIDTH * scale) / 2f
        val dy = (size.height - VIEW_BOX_HEIGHT * scale) / 2f

        val t = phaseToLerpFactor(phase.value)
        val pose = interpolatePose(spec, t)

        drawPose(
            pose = pose,
            floorLine = spec.floorLine,
            scale = scale,
            dx = dx,
            dy = dy,
            inkColor = Ink,
            amberColor = Amber,
            lineColor = Line
        )
    }
}

private fun Context.isAnimatorDurationScaleZero(): Boolean {
    return Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}

private fun interpolatePose(spec: FigureSpec, t: Float): Pose {
    val baseSegments = lerpSegments(spec.rest.segments, spec.contracted.segments, t)
    val baseCircles = lerpCircles(spec.rest.circles, spec.contracted.circles, t)
    val baseWeightSegments = lerpSegments(spec.rest.weightSegments, spec.contracted.weightSegments, t)
    val baseWeightCircles = lerpCircles(spec.rest.weightCircles, spec.contracted.weightCircles, t)

    if (spec.hingeAngle == 0f) {
        return Pose(
            segments = baseSegments,
            circles = baseCircles,
            weightSegments = baseWeightSegments,
            weightCircles = baseWeightCircles
        )
    }

    // For the Romanian deadlift, rotate the upper body group around the hip
    // pivot by an eased angle from 0..hingeAngle..0 over the loop.
    val angle = t * spec.hingeAngle
    return Pose(
        segments = baseSegments.mapIndexed { index, segment ->
            if (index in spec.rest.upperBodySegmentIndices) {
                segment.rotate(angle, spec.pivot)
            } else {
                segment
            }
        },
        circles = baseCircles.mapIndexed { index, circle ->
            if (index in spec.rest.upperBodyCircleIndices) {
                circle.rotate(angle, spec.pivot)
            } else {
                circle
            }
        },
        weightSegments = baseWeightSegments.mapIndexed { index, segment ->
            if (index in spec.rest.upperBodyWeightSegmentIndices) {
                segment.rotate(angle, spec.pivot)
            } else {
                segment
            }
        },
        weightCircles = baseWeightCircles.mapIndexed { index, circle ->
            if (index in spec.rest.upperBodyWeightCircleIndices) {
                circle.rotate(angle, spec.pivot)
            } else {
                circle
            }
        }
    )
}

private fun lerpSegments(
    start: List<Segment>,
    end: List<Segment>,
    t: Float
): List<Segment> = start.zip(end) { a, b -> a.lerp(b, t) }

private fun lerpCircles(
    start: List<Circle>,
    end: List<Circle>,
    t: Float
): List<Circle> = start.zip(end) { a, b ->
    Circle(
        center = a.center.lerp(b.center, t),
        radius = a.radius + (b.radius - a.radius) * t,
        filled = a.filled
    )
}

private fun DrawScope.drawPose(
    pose: Pose,
    floorLine: Segment?,
    scale: Float,
    dx: Float,
    dy: Float,
    inkColor: Color,
    amberColor: Color,
    lineColor: Color
) {
    fun pt(offset: Offset) = Offset(dx + offset.x * scale, dy + offset.y * scale)

    floorLine?.let {
        drawLine(lineColor, pt(it.start), pt(it.end), strokeWidth = 3f * scale)
    }

    val figureStrokeWidth = 4f * scale
    val weightStrokeWidth = 7f * scale

    pose.segments.forEach { segment ->
        drawLine(
            color = inkColor,
            start = pt(segment.start),
            end = pt(segment.end),
            strokeWidth = figureStrokeWidth,
            cap = StrokeCap.Round
        )
    }

    pose.weightSegments.forEach { segment ->
        drawLine(
            color = amberColor,
            start = pt(segment.start),
            end = pt(segment.end),
            strokeWidth = weightStrokeWidth,
            cap = StrokeCap.Round
        )
    }

    pose.weightCircles.forEach { circle ->
        drawCircle(
            color = amberColor,
            radius = circle.radius * scale,
            center = pt(circle.center)
        )
    }

    pose.circles.forEach { circle ->
        if (circle.filled) {
            drawCircle(
                color = inkColor,
                radius = circle.radius * scale,
                center = pt(circle.center)
            )
        } else {
            drawCircle(
                color = inkColor,
                radius = circle.radius * scale,
                center = pt(circle.center),
                style = Stroke(width = 4f * scale)
            )
        }
    }
}
