package com.nkapila.workout.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "muscle_group")
    val muscleGroup: String,
    val cue: String,
    @ColumnInfo(name = "animation_key")
    val animationKey: String,
    val difficulty: String,
    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false
)

@Entity(
    tableName = "routine_items",
    indices = [Index(value = ["routine_id"])]
)
data class RoutineItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "routine_id")
    val routineId: String,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    val sets: Int,
    @ColumnInfo(name = "rep_min")
    val repMin: Int,
    @ColumnInfo(name = "rep_max")
    val repMax: Int,
    @ColumnInfo(name = "per_side")
    val perSide: Boolean = false
)

data class RoutineWithItems(
    @Embedded
    val routine: RoutineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routine_id",
        entity = RoutineItemEntity::class
    )
    val items: List<RoutineItemEntity>
)

@Entity(
    tableName = "session_logs",
    indices = [Index(value = ["routine_id"])]
)
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    @ColumnInfo(name = "routine_id")
    val routineId: String
)

@Entity(
    tableName = "set_entries",
    indices = [Index(value = ["session_id"])]
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,
    @ColumnInfo(name = "set_number")
    val setNumber: Int,
    val reps: Int,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double
)

data class SessionWithEntries(
    @Embedded
    val session: SessionLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id",
        entity = SetEntryEntity::class
    )
    val entries: List<SetEntryEntity>
)

@Entity(tableName = "body_weights")
data class BodyWeightEntity(
    @PrimaryKey
    val date: Long,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double
)
