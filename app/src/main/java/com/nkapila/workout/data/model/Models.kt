package com.nkapila.workout.data.model

enum class MuscleGroup(val groupName: String) {
    LEGS("legs"),
    PUSH("push"),
    PULL("pull"),
    HINGE("hinge"),
    SHOULDERS("shoulders"),
    CORE("core"),
    FULL_BODY("fullbody")
}

enum class Difficulty(val value: String) {
    EASIER("easier"),
    STANDARD("standard"),
    HARDER("harder")
}

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val cue: String,
    val animationKey: String,
    val difficulty: Difficulty,
    val isCustom: Boolean = false
)

data class Routine(
    val id: String,
    val name: String,
    val isCustom: Boolean = false,
    val items: List<RoutineItem> = emptyList()
)

data class RoutineItem(
    val exerciseId: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val perSide: Boolean = false
)

data class SetLog(
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double
)

data class SessionLog(
    val id: Long,
    val date: Long,
    val routineId: String,
    val entries: List<SetLog> = emptyList()
)

data class BodyWeightEntry(
    val date: Long,
    val weightKg: Double
)

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup.groupName,
    cue = cue,
    animationKey = animationKey,
    difficulty = difficulty.value,
    isCustom = isCustom
)

fun ExerciseEntity.toModel() = Exercise(
    id = id,
    name = name,
    muscleGroup = MuscleGroup.entries.firstOrNull { it.groupName == muscleGroup } ?: MuscleGroup.FULL_BODY,
    cue = cue,
    animationKey = animationKey,
    difficulty = Difficulty.entries.firstOrNull { it.value == difficulty } ?: Difficulty.STANDARD,
    isCustom = isCustom
)

fun Routine.toEntity() = RoutineEntity(
    id = id,
    name = name,
    isCustom = isCustom
)

fun RoutineEntity.toModel(items: List<RoutineItem> = emptyList()) = Routine(
    id = id,
    name = name,
    isCustom = isCustom,
    items = items
)

fun RoutineItem.toEntity(routineId: String, orderIndex: Int) = RoutineItemEntity(
    routineId = routineId,
    exerciseId = exerciseId,
    orderIndex = orderIndex,
    sets = sets,
    repMin = repMin,
    repMax = repMax,
    perSide = perSide
)

fun RoutineItemEntity.toModel() = RoutineItem(
    exerciseId = exerciseId,
    sets = sets,
    repMin = repMin,
    repMax = repMax,
    perSide = perSide
)

fun SetLog.toEntity(sessionId: Long) = SetEntryEntity(
    sessionId = sessionId,
    exerciseId = exerciseId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg
)

fun SetEntryEntity.toModel() = SetLog(
    exerciseId = exerciseId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg
)

fun SessionLog.toEntity() = SessionLogEntity(
    id = id,
    date = date,
    routineId = routineId
)

fun SessionLogEntity.toModel(entries: List<SetLog> = emptyList()) = SessionLog(
    id = id,
    date = date,
    routineId = routineId,
    entries = entries
)

fun BodyWeightEntry.toEntity() = BodyWeightEntity(
    date = date,
    weightKg = weightKg
)

fun BodyWeightEntity.toModel() = BodyWeightEntry(
    date = date,
    weightKg = weightKg
)
