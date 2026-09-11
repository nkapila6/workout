package com.nkapila.workout.ui.animation

import androidx.compose.ui.geometry.Offset

/**
 * A simple geometric primitive list for a stick figure.
 * All coordinates live in the SVG viewBox space 0..120 x 0..140.
 */
internal data class Pose(
    val segments: List<Segment>,
    val circles: List<Circle>,
    val weightSegments: List<Segment> = emptyList(),
    val weightCircles: List<Circle> = emptyList(),
    // Optional set of segment indices that belong to the upper body and
    // should be rotated as a group for the RDL pose.
    val upperBodySegmentIndices: Set<Int> = emptySet(),
    val upperBodyCircleIndices: Set<Int> = emptySet(),
    val upperBodyWeightSegmentIndices: Set<Int> = emptySet(),
    val upperBodyWeightCircleIndices: Set<Int> = emptySet()
)

internal data class Segment(
    val start: Offset,
    val end: Offset
)

internal data class Circle(
    val center: Offset,
    val radius: Float,
    val filled: Boolean = false
)

internal data class FigureSpec(
    val rest: Pose,
    val contracted: Pose,
    val periodMillis: Int,
    val pivot: Offset = Offset.Zero,
    // For RDL: the maximum hinge angle in degrees.
    val hingeAngle: Float = 0f,
    val floorLine: Segment? = null
)

private fun s(x1: Float, y1: Float, x2: Float, y2: Float) = Segment(Offset(x1, y1), Offset(x2, y2))
private fun c(cx: Float, cy: Float, r: Float, filled: Boolean = true) = Circle(Offset(cx, cy), r, filled)
private fun rectToSegments(cx: Float, cy: Float, w: Float, h: Float): List<Segment> {
    val left = cx - w / 2f
    val right = cx + w / 2f
    val top = cy - h / 2f
    val bottom = cy + h / 2f
    return listOf(
        s(left, top, right, top),
        s(right, top, right, bottom),
        s(right, bottom, left, bottom),
        s(left, bottom, left, top)
    )
}

internal val figures: Map<String, FigureSpec> = mapOf(
    "goblet_squat" to FigureSpec(
        periodMillis = 2600,
        floorLine = s(20f, 126f, 100f, 126f),
        rest = Pose(
            segments = listOf(
                // Upper body group: torso, arms, and head. These translate down
            // by 22 units in the contracted pose while feet stay planted.
                s(60f, 30f, 60f, 70f),          // torso
                s(60f, 42f, 49f, 54f),          // upper arm L
                s(60f, 42f, 71f, 54f),          // upper arm R
                s(49f, 54f, 60f, 58f),          // forearm L
                s(71f, 54f, 60f, 58f),          // forearm R
                s(60f, 70f, 50f, 98f),          // thigh L
                s(60f, 70f, 70f, 98f),          // thigh R
                s(50f, 98f, 50f, 126f),         // shin L
                s(70f, 98f, 70f, 126f),         // shin R
                s(50f, 126f, 60f, 126f),        // foot L
                s(70f, 126f, 60f, 126f)         // foot R
            ),
            circles = listOf(
                c(60f, 20f, 9f)                 // head
            ),
            weightSegments = rectToSegments(60f, 56f, 12f, 12f),
            upperBodySegmentIndices = setOf(0, 1, 2, 3, 4),
            upperBodyCircleIndices = setOf(0),
            upperBodyWeightSegmentIndices = (0..3).toSet()
        ),
        contracted = Pose(
            segments = listOf(
                s(60f, 52f, 60f, 92f),          // torso lowered by 22
                s(60f, 64f, 49f, 76f),
                s(60f, 64f, 71f, 76f),
                s(49f, 76f, 60f, 80f),
                s(71f, 76f, 60f, 80f),
                s(60f, 92f, 42f, 108f),         // thigh L
                s(60f, 92f, 78f, 108f),         // thigh R
                s(42f, 108f, 42f, 126f),        // shin L
                s(78f, 108f, 78f, 126f),        // shin R
                s(42f, 126f, 60f, 126f),
                s(78f, 126f, 60f, 126f)
            ),
            circles = listOf(
                c(60f, 42f, 9f)                 // head lowered by 22
            ),
            weightSegments = rectToSegments(60f, 78f, 12f, 12f),
            upperBodySegmentIndices = setOf(0, 1, 2, 3, 4),
            upperBodyCircleIndices = setOf(0),
            upperBodyWeightSegmentIndices = (0..3).toSet()
        )
    ),

    "floor_press" to FigureSpec(
        periodMillis = 2200,
        floorLine = s(14f, 104f, 106f, 104f),
        rest = Pose(
            segments = listOf(
                s(34f, 98f, 78f, 98f),          // torso
                s(78f, 98f, 96f, 102f),         // leg upper
                s(78f, 98f, 96f, 90f)           // leg lower
            ),
            circles = listOf(
                c(26f, 96f, 8f)                 // head
            ),
            // Pressing arms + weight. These translate as a group.
            weightSegments = listOf(
                s(52f, 96f, 52f, 70f),
                s(62f, 96f, 62f, 70f),
                s(46f, 70f, 68f, 70f)
            ),
            weightCircles = listOf(
                c(46f, 70f, 5f),
                c(68f, 70f, 5f)
            )
        ),
        contracted = Pose(
            segments = listOf(
                s(34f, 98f, 78f, 98f),
                s(78f, 98f, 96f, 102f),
                s(78f, 98f, 96f, 90f)
            ),
            circles = listOf(
                c(26f, 96f, 8f)
            ),
            weightSegments = listOf(
                s(52f, 70f, 52f, 44f),          // up 26
                s(62f, 70f, 62f, 44f),
                s(46f, 44f, 68f, 44f)
            ),
            weightCircles = listOf(
                c(46f, 44f, 5f),
                c(68f, 44f, 5f)
            )
        )
    ),

    "one_arm_row" to FigureSpec(
        periodMillis = 2200,
        floorLine = s(20f, 126f, 100f, 126f),
        rest = Pose(
            segments = listOf(
                s(40f, 50f, 82f, 66f),          // torso
                s(60f, 58f, 62f, 88f),          // braced arm
                s(82f, 66f, 80f, 96f),          // thigh front
                s(80f, 96f, 80f, 126f),         // shin front
                s(82f, 66f, 92f, 96f),         // thigh back
                s(92f, 96f, 92f, 126f),        // shin back
                s(48f, 54f, 48f, 86f)          // rowing arm
            ),
            circles = listOf(
                c(34f, 46f, 8f)                 // head
            ),
            weightSegments = listOf(
                s(42f, 86f, 54f, 86f)
            ),
            weightCircles = listOf(
                c(42f, 86f, 5f),
                c(54f, 86f, 5f)
            )
        ),
        contracted = Pose(
            segments = listOf(
                s(40f, 50f, 82f, 66f),
                s(60f, 58f, 62f, 88f),
                s(82f, 66f, 80f, 96f),
                s(80f, 96f, 80f, 126f),
                s(82f, 66f, 92f, 96f),
                s(92f, 96f, 92f, 126f),
                s(48f, 54f, 48f, 64f)          // elbow drives up 22
            ),
            circles = listOf(
                c(34f, 46f, 8f)
            ),
            weightSegments = listOf(
                s(42f, 64f, 54f, 64f)
            ),
            weightCircles = listOf(
                c(42f, 64f, 5f),
                c(54f, 64f, 5f)
            )
        )
    ),

    "romanian_deadlift" to FigureSpec(
        periodMillis = 2800,
        floorLine = s(20f, 126f, 100f, 126f),
        pivot = Offset(60f, 80f),
        hingeAngle = 62f,
        rest = Pose(
            segments = listOf(
                s(60f, 80f, 56f, 102f),         // leg L (fixed)
                s(56f, 102f, 56f, 126f),        // shin L (fixed)
                s(60f, 80f, 64f, 102f),         // leg R (fixed)
                s(64f, 102f, 64f, 126f),        // shin R (fixed)
                s(60f, 80f, 60f, 36f),          // torso (hinges around hip)
                s(60f, 44f, 52f, 72f),          // arm L
                s(60f, 44f, 68f, 72f)           // arm R
            ),
            circles = listOf(
                c(60f, 27f, 9f)                 // head
            ),
            weightSegments = listOf(
                s(46f, 72f, 58f, 72f),
                s(62f, 72f, 74f, 72f)
            ),
            weightCircles = listOf(
                c(46f, 72f, 5f),
                c(58f, 72f, 5f),
                c(62f, 72f, 5f),
                c(74f, 72f, 5f)
            ),
            upperBodySegmentIndices = setOf(4, 5, 6),
            upperBodyCircleIndices = setOf(0),
            upperBodyWeightSegmentIndices = (0..1).toSet(),
            upperBodyWeightCircleIndices = (0..3).toSet()
        ),
        contracted = Pose(
            // Contracted values are ignored for the hinged upper body; we
            // compute it from rest + hinge angle. But keep the structure so
            // non-hinged segments still lerp normally.
            segments = listOf(
                s(60f, 80f, 56f, 102f),
                s(56f, 102f, 56f, 126f),
                s(60f, 80f, 64f, 102f),
                s(64f, 102f, 64f, 126f),
                s(60f, 80f, 60f, 36f),
                s(60f, 44f, 52f, 72f),
                s(60f, 44f, 68f, 72f)
            ),
            circles = listOf(
                c(60f, 27f, 9f)
            ),
            weightSegments = listOf(
                s(46f, 72f, 58f, 72f),
                s(62f, 72f, 74f, 72f)
            ),
            weightCircles = listOf(
                c(46f, 72f, 5f),
                c(58f, 72f, 5f),
                c(62f, 72f, 5f),
                c(74f, 72f, 5f)
            ),
            upperBodySegmentIndices = setOf(4, 5, 6),
            upperBodyCircleIndices = setOf(0),
            upperBodyWeightSegmentIndices = (0..1).toSet(),
            upperBodyWeightCircleIndices = (0..3).toSet()
        )
    ),

    "shoulder_press" to FigureSpec(
        periodMillis = 2200,
        floorLine = s(20f, 126f, 100f, 126f),
        rest = Pose(
            segments = listOf(
                s(60f, 40f, 60f, 82f),          // torso
                s(60f, 82f, 52f, 126f),         // leg L
                s(60f, 82f, 68f, 126f),         // leg R
                s(60f, 46f, 44f, 60f),          // upper arm L
                s(60f, 46f, 76f, 60f),          // upper arm R
                s(44f, 60f, 44f, 48f),          // forearm L
                s(76f, 60f, 76f, 48f)           // forearm R
            ),
            circles = listOf(
                c(60f, 30f, 9f)                 // head
            ),
            weightCircles = listOf(
                c(44f, 46f, 5f),
                c(76f, 46f, 5f)
            )
        ),
        contracted = Pose(
            segments = listOf(
                s(60f, 40f, 60f, 82f),
                s(60f, 82f, 52f, 126f),
                s(60f, 82f, 68f, 126f),
                s(60f, 46f, 54f, 60f),          // upper arms angle inward
                s(60f, 46f, 66f, 60f),
                s(54f, 60f, 54f, 48f),          // forearms follow inward
                s(66f, 60f, 66f, 48f)
            ),
            circles = listOf(
                c(60f, 30f, 9f)
            ),
            weightCircles = listOf(
                c(54f, 46f, 5f),
                c(66f, 46f, 5f)
            )
        )
    )
)

internal fun Offset.lerp(other: Offset, t: Float): Offset =
    Offset(x + (other.x - x) * t, y + (other.y - y) * t)

internal fun Segment.lerp(other: Segment, t: Float): Segment =
    Segment(start.lerp(other.start, t), end.lerp(other.end, t))

internal fun Segment.rotate(degrees: Float, pivot: Offset): Segment =
    Segment(start.rotate(degrees, pivot), end.rotate(degrees, pivot))

internal fun Circle.rotate(degrees: Float, pivot: Offset): Circle =
    copy(center = center.rotate(degrees, pivot))

internal fun Offset.rotate(degrees: Float, pivot: Offset): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    val sin = kotlin.math.sin(rad).toFloat()
    val cos = kotlin.math.cos(rad).toFloat()
    val dx = x - pivot.x
    val dy = y - pivot.y
    return Offset(pivot.x + dx * cos - dy * sin, pivot.y + dx * sin + dy * cos)
}
