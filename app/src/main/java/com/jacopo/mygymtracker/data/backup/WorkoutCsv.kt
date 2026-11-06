package com.jacopo.mygymtracker.data.backup

import com.jacopo.mygymtracker.domain.model.WorkoutSession
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val HEADER = "date,workout,exercise,set,type,weight_kg,reps,duration_s,rpe"

/** Spreadsheets read this as a date-time; ISO's "T" separator they keep as text. */
private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * One row per set of [workouts], for spreadsheets: an export only, the app can't read it back.
 * Weights stay in kg whatever the unit setting, so the column means one thing; `set` counts every set, warm-ups too.
 */
fun workoutsToCsv(workouts: List<WorkoutSession>, zone: ZoneId): String = buildString {
    append(HEADER).append("\r\n")
    workouts.forEach { workout ->
        val date = dateFormat.format(workout.startedAt.atZone(zone))
        workout.exercises.forEach { exercise ->
            exercise.sets.forEachIndexed { index, set ->
                val fields = listOf(
                    date,
                    workout.routineName,
                    exercise.exercise.name,
                    index + 1,
                    set.type,
                    set.weightKg,
                    set.reps,
                    set.durationSeconds,
                    set.rpe ?: "",
                )
                fields.joinTo(this, separator = ",") { it.toString().toCsvField() }
                append("\r\n")
            }
        }
    }
}

/** RFC 4180: a field holding a comma, quote or line break goes in quotes, with its quotes doubled. */
private fun String.toCsvField(): String =
    if (any { it in ",\"\r\n" }) "\"${replace("\"", "\"\"")}\"" else this
