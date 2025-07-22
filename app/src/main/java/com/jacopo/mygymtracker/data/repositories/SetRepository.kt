package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.SetDao
import com.jacopo.mygymtracker.data.model.ExerciseSet
import com.jacopo.mygymtracker.model.Set as ExerciseSetDomain

class SetRepository(
    private val setDao: SetDao
) {
    suspend fun getAllSets(): List<ExerciseSet> {
        return setDao.getAll()
    }

    suspend fun insertSets(sets: List<ExerciseSet>): List<Long> {
        return setDao.insert(sets)
    }

    suspend fun deleteSets(sets: List<ExerciseSet>) {
        setDao.delete(sets)
    }
}

fun ExerciseSet.toDomainModel(): ExerciseSetDomain {
    return ExerciseSetDomain(
        id = this.id,
        reps = this.reps,
        weight = this.weight
    )
}