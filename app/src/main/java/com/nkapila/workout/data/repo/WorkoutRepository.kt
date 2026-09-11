package com.nkapila.workout.data.repo

import com.nkapila.workout.data.db.WorkoutDao
import com.nkapila.workout.data.model.BodyWeightEntry
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.Routine
import com.nkapila.workout.data.model.RoutineItem
import com.nkapila.workout.data.model.SessionLog
import com.nkapila.workout.data.model.SessionLogEntity
import com.nkapila.workout.data.model.SetLog
import com.nkapila.workout.data.model.toEntity
import com.nkapila.workout.data.model.toModel
import com.nkapila.workout.data.seed.SeedData
import com.nkapila.workout.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class WorkoutRepository(
    private val dao: WorkoutDao,
    private val settings: SettingsRepository
) {

    suspend fun seedIfEmpty() {
        if (dao.getExercisesByIds(listOf("goblet_squat")).isNotEmpty()) return

        SeedData.exercises.forEach { dao.upsertExercise(it.toEntity()) }

        dao.upsertRoutine(SeedData.DEFAULT_ROUTINE.toEntity())
        val items = SeedData.DEFAULT_ROUTINE.items.mapIndexed { index, item ->
            item.toEntity(SeedData.DEFAULT_ROUTINE_ID, index)
        }
        dao.upsertRoutineItems(items)
    }

    fun observeRoutines(): Flow<List<Routine>> = dao.observeRoutines().map { list ->
        list.map { it.routine.toModel(it.items.map { item -> item.toModel() }) }
    }

    suspend fun getRoutine(id: String): Routine? {
        val raw = dao.getRoutineWithItems(id) ?: return null
        return raw.routine.toModel(raw.items.map { it.toModel() })
    }

    fun observeAllExercises(): Flow<List<Exercise>> =
        dao.observeAllExercises().map { list -> list.map { it.toModel() } }

    suspend fun getExercise(id: String): Exercise? =
        dao.getExerciseById(id)?.toModel()

    suspend fun getExercisesForMuscleGroup(group: String): List<Exercise> =
        dao.getExercisesByMuscleGroup(group).map { it.toModel() }

    suspend fun upsertExercise(exercise: Exercise) {
        dao.upsertExercise(exercise.toEntity())
    }

    suspend fun swapExercise(routineId: String, oldExerciseId: String, newExerciseId: String) {
        val routine = dao.getRoutineWithItems(routineId) ?: return
        val updated = routine.items.map { item ->
            if (item.exerciseId == oldExerciseId) item.copy(exerciseId = newExerciseId)
            else item
        }
        dao.upsertRoutineItems(updated)
    }

    suspend fun saveRoutine(routine: Routine) {
        dao.upsertRoutine(routine.toEntity())
        val items = routine.items.mapIndexed { index, item ->
            item.toEntity(routine.id, index)
        }
        dao.deleteRoutineItems(routine.id)
        dao.upsertRoutineItems(items)
    }

    suspend fun deleteRoutine(id: String) {
        val routine = dao.getRoutineWithItems(id) ?: return
        if (!routine.routine.isCustom) return
        dao.deleteRoutineItems(id)
        dao.deleteRoutineById(id)
    }

    suspend fun saveSession(entries: List<SetLog>, routineId: String) {
        val now = System.currentTimeMillis()
        val session = SessionLogEntity(id = 0, date = now, routineId = routineId)
        val sessionId = dao.insertSession(session)
        val setEntries = entries.map { it.toEntity(sessionId) }
        dao.insertSetEntries(setEntries)
    }

    fun observeSessions(): Flow<List<SessionLog>> = dao.getSessionHistory().map { list ->
        list.map { it.session.toModel(it.entries.map { entry -> entry.toModel() }) }
    }

    fun observeExerciseHistory(exerciseId: String): Flow<List<SessionLog>> =
        dao.getSessionsForExercise(exerciseId).map { list ->
            list.map { it.session.toModel(it.entries.map { entry -> entry.toModel() }) }
        }

    suspend fun lastEntryFor(exerciseId: String): SetLog? {
        val session = dao.getLastSessionWithEntryForExercise(exerciseId) ?: return null
        return session.entries
            .filter { it.exerciseId == exerciseId }
            .maxByOrNull { it.setNumber }
            ?.toModel()
    }

    fun sessionsInCurrentWeek(): Flow<Int> {
        val (start, end) = currentWeekRangeMillis()
        return dao.getSessionsBetween(start, end).map { it.size }
    }

    fun observeBodyWeights(): Flow<List<BodyWeightEntry>> =
        dao.observeBodyWeights().map { list -> list.map { it.toModel() } }

    suspend fun saveBodyWeight(weightKg: Double) {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.upsertBodyWeight(BodyWeightEntry(today, weightKg).toEntity())
    }

    suspend fun shouldSuggestWeightIncrease(exerciseId: String, repMax: Int): Boolean {
        val flow = dao.getSessionsForExercise(exerciseId).map { list ->
            list.map { it.session.toModel(it.entries.map { entry -> entry.toModel() }) }
                .filter { session -> session.entries.any { it.exerciseId == exerciseId } }
                .take(2)
                .all { session ->
                    session.entries
                        .filter { it.exerciseId == exerciseId }
                        .all { it.reps >= repMax }
                }
        }
        return flow.first()
    }

    private fun currentWeekRangeMillis(): Pair<Long, Long> {
        val now = LocalDate.now()
        val daysFromMonday = (now.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
        val monday = now.minusDays(daysFromMonday).atStartOfDay(ZoneId.systemDefault())
        val nextMonday = monday.plus(1, ChronoUnit.WEEKS)
        return monday.toInstant().toEpochMilli() to nextMonday.toInstant().toEpochMilli()
    }
}
