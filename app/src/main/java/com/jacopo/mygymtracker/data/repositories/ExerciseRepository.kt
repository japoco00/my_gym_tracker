package com.jacopo.mygymtracker.data.repositories

import com.jacopo.mygymtracker.data.daos.ExerciseDao
import com.jacopo.mygymtracker.data.model.Exercise
import com.jacopo.mygymtracker.data.model.ExerciseWithSets
import kotlinx.coroutines.flow.Flow
import com.jacopo.mygymtracker.model.Exercise as ExerciseDomain

class ExerciseRepository(
    private val exerciseDao: ExerciseDao
) {
    suspend fun getAllExercises(): Flow<List<ExerciseWithSets>> {
        return exerciseDao.getAll()
    }

    suspend fun insertExercises(exercises: List<Exercise>): List<Long> {
        return exerciseDao.insert(exercises)
    }

    suspend fun deleteExercises(exercises: List<Exercise>) {
        exerciseDao.delete(exercises)
    }
}

fun ExerciseWithSets.toDomainModel(): ExerciseDomain {
    return ExerciseDomain(
        id = exercise.id,
        name = exercise.name,
        note = exercise.note,
        sets = sets.map { it.toDomainModel() }.toMutableList(),
        category = exercise.category
    )
}