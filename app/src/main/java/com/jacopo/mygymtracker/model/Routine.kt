package com.jacopo.mygymtracker.model

import com.jacopo.mygymtracker.data.model.RoutineEntry

data class Routine(
    var id: Int? = null,
    val name: String,
    val exercises: List<Exercise> = emptyList(),
    var note: String? = null
)

fun Routine.toEntry(): RoutineEntry {
    return RoutineEntry(
        routineId = id ?: 0,
        name = name,
        note = note
    )
}

fun RoutineEntry.toModel(exercises: List<Exercise> = emptyList()): Routine {
    return Routine(
        id = routineId,
        name = name,
        note = note,
        exercises = exercises
    )
}
