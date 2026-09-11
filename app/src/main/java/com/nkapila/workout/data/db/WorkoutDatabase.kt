package com.nkapila.workout.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.nkapila.workout.data.model.BodyWeightEntity
import com.nkapila.workout.data.model.ExerciseEntity
import com.nkapila.workout.data.model.RoutineEntity
import com.nkapila.workout.data.model.RoutineItemEntity
import com.nkapila.workout.data.model.SessionLogEntity
import com.nkapila.workout.data.model.SetEntryEntity

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineItemEntity::class,
        SessionLogEntity::class,
        SetEntryEntity::class,
        BodyWeightEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val NAME = "workout_database"

        fun create(context: Context): WorkoutDatabase =
            Room.databaseBuilder(context, WorkoutDatabase::class.java, NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
