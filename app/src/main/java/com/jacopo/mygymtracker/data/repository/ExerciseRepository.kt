package com.jacopo.mygymtracker.data.repository

import com.jacopo.mygymtracker.data.local.dao.ExerciseDao
import com.jacopo.mygymtracker.data.mapper.toDomain
import com.jacopo.mygymtracker.data.mapper.toEntity
import com.jacopo.mygymtracker.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Access to the reusable exercise catalog. */
class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
) {
    /** Emits the whole catalog, sorted by name. */
    fun observeCatalog(): Flow<List<Exercise>> =
        exerciseDao.observeAll().map { exercises -> exercises.map { it.toDomain() } }
}

/**
 * Returns the catalog id of [exercise]. An unsaved exercise reuses the catalog entry with the
 * same name (ignoring case), or is added to the catalog. Saved exercises are never modified here.
 * Call inside a transaction.
 */
internal suspend fun ExerciseDao.resolveCatalogId(exercise: Exercise): Int =
    exercise.id
        ?: findByName(exercise.name)?.exerciseId
        ?: insert(exercise.toEntity()).toInt()