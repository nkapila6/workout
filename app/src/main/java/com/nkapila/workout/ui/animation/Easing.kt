package com.nkapila.workout.ui.animation

import kotlin.math.abs

/**
 * Cubic-bezier easing matching the SVG SMIL keySplines:
 * "0.4 0 0.2 1". Given an x progress in [0,1], returns the eased y value.
 */
fun cubicBezierYforX(
    x: Float,
    p1x: Float = 0.4f,
    p1y: Float = 0f,
    p2x: Float = 0.2f,
    p2y: Float = 1f
): Float {
    val t = solveCubicBezierT(x, p1x, p2x)
    return cubicBezier(t, p1y, p2y)
}

/**
 * Map the looping animation phase (0..1) to a lerp factor that eases
 * A -> B on the first half and B -> A on the second half.
 */
fun phaseToLerpFactor(phase: Float): Float {
    val p = phase.coerceIn(0f, 1f)
    return if (p < 0.5f) {
        cubicBezierYforX(p * 2f)
    } else {
        1f - cubicBezierYforX((p - 0.5f) * 2f)
    }
}

private fun cubicBezier(t: Float, p1: Float, p2: Float): Float {
    val oneMinusT = 1f - t
    return 3f * oneMinusT * oneMinusT * t * p1 +
            3f * oneMinusT * t * t * p2 +
            t * t * t
}

private fun derivativeCubicBezier(t: Float, p1x: Float, p2x: Float): Float {
    val oneMinusT = 1f - t
    return 3f * oneMinusT * oneMinusT * p1x +
            6f * oneMinusT * t * (p2x - p1x) +
            3f * t * t * (1f - p2x)
}

private fun solveCubicBezierT(x: Float, p1x: Float, p2x: Float): Float {
    // Newton-Raphson first, then binary search as a safety net.
    var t = x
    repeat(8) {
        val xAtT = cubicBezier(t, p1x, p2x)
        val dx = derivativeCubicBezier(t, p1x, p2x)
        if (abs(dx) < 1e-6f) return@repeat
        val next = t - (xAtT - x) / dx
        if (abs(next - t) < 1e-6f) {
            t = next
            return@repeat
        }
        t = next
    }

    var low = 0f
    var high = 1f
    t = t.coerceIn(low, high)
    while (high - low > 1e-6f) {
        val mid = (low + high) / 2f
        val xMid = cubicBezier(mid, p1x, p2x)
        if (xMid < x) {
            low = mid
        } else {
            high = mid
        }
    }
    return ((low + high) / 2f).coerceIn(0f, 1f)
}
