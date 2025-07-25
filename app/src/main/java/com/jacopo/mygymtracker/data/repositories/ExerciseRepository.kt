package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.ExerciseDao
import com.jacopo.mygymtracker.data.model.ExerciseEntry
import com.jacopo.mygymtracker.data.model.ExerciseWithSetsEntry
import com.jacopo.mygymtracker.model.Exercise

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val setRepository: SetRepository
) {
    suspend fun updateFull(exercise: Exercise, routineId: Int) {
        val exerciseEntry = exercise.toDataModel(routineId)
        val id = exerciseDao.insert(exerciseEntry).toInt()
        for (set in exercise.sets) {
            setRepository.update(set, id)
        }
    }
}

fun ExerciseWithSetsEntry.toDomainModel(): Exercise {
    return Exercise(
        id = exerciseEntry.id,
        name = exerciseEntry.name,
        note = exerciseEntry.note,
        sets = sets.map { it.toDomainModel() }.toMutableList(),
        category = exerciseEntry.category
    )
}

fun Exercise.toDataModel(routineId: Int): ExerciseEntry {
    return ExerciseEntry(
        id = id ?: 0, // Assuming id is nullable, use 0 for new exercises
        name = name,
        note = note,
        category = category,
        routineId = routineId
    )
}