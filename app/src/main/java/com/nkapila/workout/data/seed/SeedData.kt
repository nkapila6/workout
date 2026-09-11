package com.nkapila.workout.data.seed

import com.nkapila.workout.data.model.Difficulty
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.MuscleGroup
import com.nkapila.workout.data.model.Routine
import com.nkapila.workout.data.model.RoutineItem

private fun ex(
    id: String,
    name: String,
    muscleGroup: MuscleGroup,
    difficulty: Difficulty = Difficulty.STANDARD,
    cue: String,
    animationKey: String = "generic",
    isCustom: Boolean = false
) = Exercise(id, name, muscleGroup, cue, animationKey, difficulty, isCustom)

object SeedData {

    const val DEFAULT_ROUTINE_ID = "default_full_body"

    val exercises = listOf(
        // Legs
        ex(
            id = "goblet_squat",
            name = "Goblet squat",
            muscleGroup = MuscleGroup.LEGS,
            difficulty = Difficulty.STANDARD,
            cue = "Hold one dumbbell at your chest. Sit down between your knees, chest up, heels flat.",
            animationKey = "goblet_squat"
        ),
        ex(
            id = "bodyweight_squat",
            name = "Bodyweight squat",
            muscleGroup = MuscleGroup.LEGS,
            difficulty = Difficulty.EASIER,
            cue = "Feet shoulder-width, sit back, keep chest up and knees tracking over toes."
        ),
        ex(
            id = "box_squat",
            name = "Box squat",
            muscleGroup = MuscleGroup.LEGS,
            difficulty = Difficulty.EASIER,
            cue = "Sit back to a chair, pause lightly, stand up tall without rocking forward."
        ),
        ex(
            id = "tempo_goblet_squat",
            name = "Tempo goblet squat",
            muscleGroup = MuscleGroup.LEGS,
            difficulty = Difficulty.HARDER,
            cue = "Lower for 3 seconds, pause at the bottom, then drive up through your heels."
        ),
        ex(
            id = "bulgarian_split_squat",
            name = "Bulgarian split squat",
            muscleGroup = MuscleGroup.LEGS,
            difficulty = Difficulty.HARDER,
            cue = "Rear foot elevated, torso upright, lower until your back knee nearly touches."
        ),
        ex(
            id = "front_rack_squat",
            name = "Front-rack squat",
            muscleGroup = MuscleGroup.LEGS,
            difficulty = Difficulty.HARDER,
            cue = "Dumbbells at your shoulders, elbows high, squat deep with an upright torso."
        ),

        // Push
        ex(
            id = "floor_press",
            name = "Dumbbell floor press",
            muscleGroup = MuscleGroup.PUSH,
            difficulty = Difficulty.STANDARD,
            cue = "Lie on the floor, press dumbbells straight up until your arms lock out.",
            animationKey = "floor_press"
        ),
        ex(
            id = "incline_pushup",
            name = "Incline push-up",
            muscleGroup = MuscleGroup.PUSH,
            difficulty = Difficulty.EASIER,
            cue = "Hands on a sturdy couch or table, body straight, lower chest to the edge."
        ),
        ex(
            id = "knee_pushup",
            name = "Knee push-up",
            muscleGroup = MuscleGroup.PUSH,
            difficulty = Difficulty.EASIER,
            cue = "Knees on the floor, body straight from hips to shoulders, lower and press up."
        ),
        ex(
            id = "single_arm_floor_press",
            name = "Single-arm floor press",
            muscleGroup = MuscleGroup.PUSH,
            difficulty = Difficulty.HARDER,
            cue = "One dumbbell only, keep the other shoulder pinned to the floor, press straight up."
        ),
        ex(
            id = "tempo_floor_press",
            name = "Tempo floor press",
            muscleGroup = MuscleGroup.PUSH,
            difficulty = Difficulty.HARDER,
            cue = "Lower for 3 seconds, pause at the bottom, then press to lockout."
        ),
        ex(
            id = "close_grip_floor_press",
            name = "Close-grip floor press",
            muscleGroup = MuscleGroup.PUSH,
            difficulty = Difficulty.HARDER,
            cue = "Dumbbells close together, elbows tucked, press from chest to lockout."
        ),

        // Pull
        ex(
            id = "one_arm_row",
            name = "One-arm dumbbell row",
            muscleGroup = MuscleGroup.PULL,
            difficulty = Difficulty.STANDARD,
            cue = "Hinge flat, pull the dumbbell to your hip, squeeze your shoulder blade back.",
            animationKey = "one_arm_row"
        ),
        ex(
            id = "supported_two_arm_row",
            name = "Supported two-arm row",
            muscleGroup = MuscleGroup.PULL,
            difficulty = Difficulty.EASIER,
            cue = "Both hands on dumbbells, chest supported on a bench, row elbows to your ribs."
        ),
        ex(
            id = "chest_supported_row",
            name = "Chest-supported row",
            muscleGroup = MuscleGroup.PULL,
            difficulty = Difficulty.HARDER,
            cue = "Lie face down on a bench, row both dumbbells to your lower chest."
        ),
        ex(
            id = "gorilla_row",
            name = "Gorilla row",
            muscleGroup = MuscleGroup.PULL,
            difficulty = Difficulty.HARDER,
            cue = "Hinged with two dumbbells, alternate rowing one while the other stays on the floor."
        ),
        ex(
            id = "tempo_row",
            name = "Tempo row",
            muscleGroup = MuscleGroup.PULL,
            difficulty = Difficulty.HARDER,
            cue = "Pull up for 1 second, hold for 2, lower for 3. Keep your back flat."
        ),

        // Hinge
        ex(
            id = "romanian_deadlift",
            name = "Romanian deadlift",
            muscleGroup = MuscleGroup.HINGE,
            difficulty = Difficulty.STANDARD,
            cue = "Soft knees, push your hips back, keep dumbbells close to your legs, stand tall.",
            animationKey = "romanian_deadlift"
        ),
        ex(
            id = "hip_hinge_to_box",
            name = "Hip hinge to a box",
            muscleGroup = MuscleGroup.HINGE,
            difficulty = Difficulty.EASIER,
            cue = "Push hips back until dumbbells touch a box or chair, then squeeze glutes to stand."
        ),
        ex(
            id = "light_kettlebell_rdl",
            name = "Light kettlebell RDL",
            muscleGroup = MuscleGroup.HINGE,
            difficulty = Difficulty.EASIER,
            cue = "Hold a light weight with both hands, hips back, slight knee bend, stand tall."
        ),
        ex(
            id = "single_leg_rdl",
            name = "Single-leg RDL",
            muscleGroup = MuscleGroup.HINGE,
            difficulty = Difficulty.HARDER,
            cue = "Stand on one leg, hinge until your back leg and torso are nearly parallel to the floor."
        ),
        ex(
            id = "deficit_rdl",
            name = "Deficit RDL",
            muscleGroup = MuscleGroup.HINGE,
            difficulty = Difficulty.HARDER,
            cue = "Stand on a small step, lower the weights through a larger range, keep your back flat."
        ),
        ex(
            id = "tempo_rdl",
            name = "Tempo RDL",
            muscleGroup = MuscleGroup.HINGE,
            difficulty = Difficulty.HARDER,
            cue = "Lower for 3 seconds, pause at the stretch, then stand with a glute squeeze."
        ),

        // Shoulders
        ex(
            id = "shoulder_press",
            name = "Dumbbell shoulder press",
            muscleGroup = MuscleGroup.SHOULDERS,
            difficulty = Difficulty.STANDARD,
            cue = "Start at shoulder height, brace your core, press dumbbells overhead until arms lock out.",
            animationKey = "shoulder_press"
        ),
        ex(
            id = "seated_shoulder_press",
            name = "Seated shoulder press",
            muscleGroup = MuscleGroup.SHOULDERS,
            difficulty = Difficulty.EASIER,
            cue = "Sit tall with back support, press dumbbells straight up from shoulder height."
        ),
        ex(
            id = "half_kneeling_press",
            name = "Half-kneeling press",
            muscleGroup = MuscleGroup.SHOULDERS,
            difficulty = Difficulty.EASIER,
            cue = "One knee down, ribs down, press one dumbbell overhead with a stable core."
        ),
        ex(
            id = "arnold_press",
            name = "Arnold press",
            muscleGroup = MuscleGroup.SHOULDERS,
            difficulty = Difficulty.HARDER,
            cue = "Palms face you at the start, rotate as you press so they face forward at the top."
        ),
        ex(
            id = "push_press",
            name = "Push press",
            muscleGroup = MuscleGroup.SHOULDERS,
            difficulty = Difficulty.HARDER,
            cue = "A small dip at the knees, then drive the dumbbells overhead using leg drive."
        ),
        ex(
            id = "single_arm_press",
            name = "Single-arm press",
            muscleGroup = MuscleGroup.SHOULDERS,
            difficulty = Difficulty.HARDER,
            cue = "Press one dumbbell overhead at a time, keep the other side stable."
        ),

        // Core add-ons
        ex(
            id = "plank",
            name = "Plank",
            muscleGroup = MuscleGroup.CORE,
            difficulty = Difficulty.STANDARD,
            cue = "Forearms under shoulders, body straight, squeeze glutes and brace your abs."
        ),
        ex(
            id = "dead_bug",
            name = "Dead bug",
            muscleGroup = MuscleGroup.CORE,
            difficulty = Difficulty.STANDARD,
            cue = "Lie on your back, lower opposite arm and leg while keeping your low back pressed down."
        ),
        ex(
            id = "suitcase_carry",
            name = "Suitcase carry",
            muscleGroup = MuscleGroup.CORE,
            difficulty = Difficulty.STANDARD,
            cue = "Walk holding one dumbbell at your side, keep your torso upright and shoulders level."
        ),
        ex(
            id = "hollow_hold",
            name = "Hollow hold",
            muscleGroup = MuscleGroup.CORE,
            difficulty = Difficulty.STANDARD,
            cue = "Lie on your back, lift shoulders and legs, press your lower back into the floor."
        )
    )

    val DEFAULT_ROUTINE = Routine(
        id = DEFAULT_ROUTINE_ID,
        name = "Full-body strength",
        isCustom = false,
        items = listOf(
            RoutineItem("goblet_squat", sets = 3, repMin = 8, repMax = 12),
            RoutineItem("floor_press", sets = 3, repMin = 8, repMax = 12),
            RoutineItem("one_arm_row", sets = 3, repMin = 8, repMax = 12, perSide = true),
            RoutineItem("romanian_deadlift", sets = 3, repMin = 8, repMax = 12),
            RoutineItem("shoulder_press", sets = 3, repMin = 8, repMax = 12)
        )
    )

    data class WarmupStep(
        val count: Int,
        val label: String,
        val detail: String
    )

    val WARMUP = listOf(
        WarmupStep(
            count = 1,
            label = "Arm circles + shoulder rolls",
            detail = "20 arm circles each direction, then 10 shoulder rolls back and 10 forward."
        ),
        WarmupStep(
            count = 2,
            label = "Bodyweight squats",
            detail = "10 reps, slow and to full depth, keep chest up."
        ),
        WarmupStep(
            count = 3,
            label = "Hip hinges",
            detail = "10 reps, hands at hips, push butt back with a flat back."
        )
    )
}
