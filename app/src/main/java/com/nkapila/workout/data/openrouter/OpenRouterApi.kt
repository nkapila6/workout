package com.nkapila.workout.data.openrouter

import com.nkapila.workout.data.model.Difficulty
import com.nkapila.workout.data.model.Exercise
import com.nkapila.workout.data.model.MuscleGroup
import com.nkapila.workout.data.model.Routine
import com.nkapila.workout.data.model.RoutineItem
import com.nkapila.workout.data.seed.SeedData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage
)

@Serializable
data class GeneratedRoutineDto(
    val name: String,
    val exercises: List<GeneratedExerciseDto>
)

@Serializable
data class GeneratedExerciseDto(
    val name: String,
    val muscleGroup: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val perSide: Boolean = false,
    val cue: String
)

class GenerationException(message: String) : Exception(message)

private const val BASE_URL = "https://openrouter.ai/"

interface OpenRouterService {
    @POST("api/v1/chat/completions")
    suspend fun generate(
        @Header("Authorization") auth: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

fun createOpenRouterService(): OpenRouterService {
    val json = Json { ignoreUnknownKeys = true; isLenient = false }
    val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    return retrofit.create(OpenRouterService::class.java)
}

class OpenRouterGenerator(
    private val api: OpenRouterService,
    private val json: Json,
    private val keyProvider: suspend () -> String
) {

    private val systemPrompt =
        "You are a strength coach generating a home dumbbell workout. Return ONLY valid JSON matching the schema, no prose, no markdown fences. Movements must be doable at home with adjustable or fixed dumbbells and bodyweight only. Keep total exercises between 4 and 6. Use standard rep ranges (6 to 15). Every exercise needs a muscleGroup from: legs, push, pull, hinge, shoulders, core. Include a one-line form cue for each."

    suspend fun generateRoutine(userConstraints: String, model: String): GeneratedRoutineDto {
        val key = keyProvider()
        if (key.isBlank()) throw GenerationException("no_key")

        val first = callApi(model, userConstraints)
        return parseResponse(first) ?: run {
            val retry = callApi(model, "$userConstraints\n\nThat was not valid JSON matching the schema. Return only the JSON.")
            parseResponse(retry) ?: throw GenerationException("malformed")
        }
    }

    private suspend fun callApi(model: String, userContent: String): OpenRouterResponse {
        return try {
            api.generate(
                auth = "Bearer ${keyProvider()}",
                request = OpenRouterRequest(
                    model = model,
                    messages = listOf(
                        OpenRouterMessage("system", systemPrompt),
                        OpenRouterMessage("user", userContent)
                    )
                )
            )
        } catch (e: HttpException) {
            throw when (e.code()) {
                401 -> GenerationException("invalid_key")
                429 -> GenerationException("rate_limited: rate limit hit, retry later")
                in 500..599 -> GenerationException("server: server error, retry later")
                else -> GenerationException("network")
            }
        } catch (e: java.io.IOException) {
            throw GenerationException("network")
        } catch (e: Exception) {
            throw GenerationException("network")
        }
    }

    private fun parseResponse(response: OpenRouterResponse): GeneratedRoutineDto? {
        val raw = response.choices.firstOrNull()?.message?.content?.trim() ?: return null
        val stripped = raw.stripFences()
        return runCatching { json.decodeFromString<GeneratedRoutineDto>(stripped) }.getOrNull()
    }

    private fun String.stripFences(): String {
        val trimmed = this.trim()
        val regexStrip = "^```(?:[a-zA-Z]*)\\s*\\n?([\\s\\S]*?)\\n?```\\s*$".toRegex()
        regexStrip.find(trimmed)?.let { return it.groupValues[1].trim() }
        return trimmed
            .replace("```json", "", ignoreCase = true)
            .replace("```", "")
            .trim()
    }
}

fun GeneratedRoutineDto.toRoutineAndExercises(routineId: String): Pair<Routine, List<Exercise>> {
    val knownByName = SeedData.exercises.associateBy { it.name.lowercase() }

    val exercises = exercises.map { dto ->
        val slug = dto.name.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        val matched = knownByName[dto.name.lowercase()]
        Exercise(
            id = matched?.id ?: slug,
            name = dto.name,
            muscleGroup = MuscleGroup.entries.firstOrNull { it.groupName == dto.muscleGroup } ?: MuscleGroup.FULL_BODY,
            cue = dto.cue,
            animationKey = matched?.animationKey ?: "generic",
            difficulty = Difficulty.STANDARD,
            isCustom = true
        )
    }

    val routineItems = this.exercises.mapIndexed { index, dto ->
        val exercise = exercises[index]
        RoutineItem(
            exerciseId = exercise.id,
            sets = dto.sets,
            repMin = dto.repMin,
            repMax = dto.repMax,
            perSide = dto.perSide
        )
    }

    val routine = Routine(
        id = routineId,
        name = name,
        isCustom = true,
        items = routineItems
    )

    return routine to exercises
}
