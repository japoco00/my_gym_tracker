package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.SetDao
import com.jacopo.mygymtracker.data.model.SetEntry
import com.jacopo.mygymtracker.model.ExerciseSet

class SetRepository(
    private val setDao: SetDao
) {
    suspend fun update(set: ExerciseSet, exerciseId: Int) {
        setDao.insert(set.toDataModel(exerciseId))
    }
}

fun SetEntry.toDomainModel(): ExerciseSet {
    return ExerciseSet(
        id = this.id,
        reps = this.reps,
        weight = this.weight
    )
}

fun ExerciseSet.toDataModel(exerciseId: Int): SetEntry {
    return SetEntry(
        id = this.id ?: 0, // Assuming id is nullable, use 0 for new sets
        reps = this.reps,
        weight = this.weight,
        exerciseId = exerciseId
    )
}