package com.nkapila.workout.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nkapila.workout.ui.screens.ExerciseDetailScreen
import com.nkapila.workout.ui.screens.GenerateScreen
import com.nkapila.workout.ui.screens.HomeScreen
import com.nkapila.workout.ui.screens.ProgressScreen
import com.nkapila.workout.ui.screens.SessionScreen
import com.nkapila.workout.ui.screens.SettingsScreen

@Composable
fun WorkoutApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen() }

        composable(
            route = Routes.SESSION,
            arguments = listOf(
                navArgument(Routes.SESSION_ARG) {
                    type = NavType.StringType
                    defaultValue = Routes.DEFAULT_ROUTINE_ID
                }
            )
        ) { SessionScreen() }

        composable(
            route = Routes.EXERCISE,
            arguments = listOf(navArgument(Routes.EXERCISE_ARG) { type = NavType.StringType })
        ) { ExerciseDetailScreen() }

        composable(Routes.PROGRESS) { ProgressScreen() }
        composable(Routes.GENERATE) { GenerateScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
