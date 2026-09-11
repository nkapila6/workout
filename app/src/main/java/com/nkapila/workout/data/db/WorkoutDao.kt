package com.nkapila.workout.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nkapila.workout.data.model.BodyWeightEntity
import com.nkapila.workout.data.model.ExerciseEntity
import com.nkapila.workout.data.model.RoutineEntity
import com.nkapila.workout.data.model.RoutineItemEntity
import com.nkapila.workout.data.model.RoutineWithItems
import com.nkapila.workout.data.model.SessionLogEntity
import com.nkapila.workout.data.model.SessionWithEntries
import com.nkapila.workout.data.model.SetEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE muscle_group = :group ORDER BY name ASC")
    suspend fun getExercisesByMuscleGroup(group: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercisesByIds(ids: List<String>): List<ExerciseEntity>

    // Routines
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineItems(items: List<RoutineItemEntity>)

    @Transaction
    @Query("SELECT * FROM routines ORDER BY name ASC")
    fun observeRoutines(): Flow<List<RoutineWithItems>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineWithItems(id: String): RoutineWithItems?

    @Query("DELETE FROM routine_items WHERE routine_id = :routineId")
    suspend fun deleteRoutineItems(routineId: String)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: String)

    // Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetEntries(entries: List<SetEntryEntity>)

    @Transaction
    @Query("SELECT * FROM session_logs ORDER BY date DESC")
    fun getSessionHistory(): Flow<List<SessionWithEntries>>

    @Transaction
    @Query("""
        SELECT DISTINCT sl.* FROM session_logs sl
        INNER JOIN set_entries se ON se.session_id = sl.id
        WHERE se.exercise_id = :exerciseId
        ORDER BY sl.date DESC
    """)
    fun getSessionsForExercise(exerciseId: String): Flow<List<SessionWithEntries>>

    @Transaction
    @Query("""
        SELECT sl.* FROM session_logs sl
        INNER JOIN set_entries se ON se.session_id = sl.id
        WHERE se.exercise_id = :exerciseId
        ORDER BY sl.date DESC
        LIMIT 1
    """)
    suspend fun getLastSessionWithEntryForExercise(exerciseId: String): SessionWithEntries?

    @Transaction
    @Query("SELECT * FROM session_logs WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getSessionsBetween(start: Long, end: Long): Flow<List<SessionWithEntries>>

    // Body weight
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBodyWeight(entry: BodyWeightEntity)

    @Query("SELECT * FROM body_weights ORDER BY date ASC")
    fun observeBodyWeights(): Flow<List<BodyWeightEntity>>
}
