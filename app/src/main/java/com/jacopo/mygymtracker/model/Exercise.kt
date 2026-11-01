package com.jacopo.mygymtracker.model

import com.jacopo.mygymtracker.data.model.ExerciseEntry
import com.jacopo.mygymtracker.data.model.SetEntry

data class Exercise(
    val id: Int? = null,
    val note: String? = null,
    val name: String,
    val category: Category,
    var sets: MutableList<ExerciseSet> = mutableListOf(
        ExerciseSet(reps = 0, weight = 0),
        ExerciseSet(reps = 0, weight = 0),
        ExerciseSet(reps = 0, weight = 0)
    )
)

data class ExerciseSet(
    val id: Int? = null,
    var reps: Int,
    var weight: Int
)

enum class Category {
    CAT1, CAT2, CAT3, STRENGTH, CHEST, LEGS
}


fun Exercise.toEntry(): ExerciseEntry {
    return ExerciseEntry(
        exerciseId = id ?: 0,
        name = name,
        category = category,
        note = note,
    )
}

fun ExerciseEntry.toModel(sets: List<ExerciseSet> = emptyList()): Exercise {
    return Exercise(
        id = exerciseId,
        name = name,
        category = category,
        note = note,
        sets = sets.toMutableList(),
    )
}

fun ExerciseSet.toEntry(exerciseId: Int, routineId: Int): SetEntry {
    return SetEntry(
        setId = id ?: 0,
        reps = reps,
        weight = weight,
        exerciseId = exerciseId,
        routineId = routineId,
    )
}

fun SetEntry.toModel(): ExerciseSet {
    return ExerciseSet(
        id = setId,
        reps = reps,
        weight = weight
    )
}

