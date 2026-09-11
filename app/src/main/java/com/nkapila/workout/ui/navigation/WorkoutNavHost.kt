package com.nkapila.workout.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        WorkoutNavHost(navController = navController)
    }
}

@Composable
private fun WorkoutNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartSession = { routineId ->
                    navController.navigate(sessionRoute(routineId))
                },
                onProgress = { navController.navigate(Routes.PROGRESS) },
                onGenerate = { navController.navigate(Routes.GENERATE) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.SESSION,
            arguments = listOf(
                navArgument(Routes.SESSION_ARG) {
                    type = NavType.StringType
                    defaultValue = Routes.DEFAULT_ROUTINE_ID
                }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString(Routes.SESSION_ARG)
                ?: Routes.DEFAULT_ROUTINE_ID
            SessionScreen(
                routineId = routineId,
                onExit = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onExerciseDetail = { exerciseId ->
                    navController.navigate(exerciseRoute(exerciseId))
                },
            )
        }

        composable(
            route = Routes.EXERCISE,
            arguments = listOf(navArgument(Routes.EXERCISE_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString(Routes.EXERCISE_ARG) ?: return@composable
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROGRESS) { ProgressScreen() }
        composable(Routes.GENERATE) { GenerateScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
