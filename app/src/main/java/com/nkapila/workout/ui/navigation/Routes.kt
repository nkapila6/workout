package com.nkapila.workout.ui.navigation

object Routes {
    const val HOME = "home"
    const val PROGRESS = "progress"
    const val GENERATE = "generate"
    const val SETTINGS = "settings"

    const val SESSION = "session/{routineId}"
    const val SESSION_ARG = "routineId"
    const val DEFAULT_ROUTINE_ID = "default_full_body"

    const val EXERCISE = "exercise/{exerciseId}"
    const val EXERCISE_ARG = "exerciseId"
}

fun sessionRoute(routineId: String): String = "session/$routineId"

fun exerciseRoute(exerciseId: String): String = "exercise/$exerciseId"
